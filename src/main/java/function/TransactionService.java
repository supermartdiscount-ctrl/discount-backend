package function;

import Repo.AccountRepository;
import Repo.BranchRepository;
import Repo.FailedTransactionRepository;
import Repo.ItemRepo;
import Repo.MonthlySaleRepository;
import Repo.TransactionRepository;
import Request.TransactionItemRequest;
import Request.TransactionRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TransactionService {

    private static final String CODE_CHARS = "0123456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ItemRepo itemRepo;
    private final BranchRepository branchRepository;
    private final MonthlySaleRepository monthlySaleRepository;
    private final FailedTransactionRepository failedTransactionRepository;

    public TransactionService(TransactionRepository transactionRepository,
                               AccountRepository accountRepository,
                               ItemRepo itemRepo,
                               BranchRepository branchRepository,
                               MonthlySaleRepository monthlySaleRepository,
                               FailedTransactionRepository failedTransactionRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.itemRepo = itemRepo;
        this.branchRepository = branchRepository;
        this.monthlySaleRepository = monthlySaleRepository;
        this.failedTransactionRepository = failedTransactionRepository;
    }

    public List<Monthly> loadDailyNetSalesForBranchAndMonth(Long branchId, YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<MonthlySale> archives =
                monthlySaleRepository.findByBranch_IdAndDayBetweenOrderByDayAsc(branchId, start, end);

        List<Monthly> results = new ArrayList<>();
        for (MonthlySale m : archives) {
            results.add(new Monthly(
                    m.getDay().toString(),
                    m.getTotalSales(),
                    m.getTotalCash(),
                    m.getTotalGCash(),
                    m.getTotalCredit(),
                    m.getTransactionCount(),
                    m.getItemsBreakdownJson()
            ));
        }
        return results;
    }

    @Transactional
    public MonthlySale archiveAndClearDay(Long branchId, LocalDate day) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalStateException("Branch not found: " + branchId));

        List<Transaction> transactions = loadTransactionsForBranchAndDate(branchId, day);
        if (transactions.isEmpty()) {
            throw new IllegalStateException(
                    "No transactions found for this branch on " + day + ". Nothing to archive.");
        }

        double totalSales = 0.0, totalCash = 0.0, totalGCash = 0.0, totalCredit = 0.0;
        Map<String, double[]> itemAgg = new LinkedHashMap<>();

        for (Transaction t : transactions) {
            totalSales += t.getTotalAmount();
            String method = t.getPaymentMethod() == null ? "" : t.getPaymentMethod().toUpperCase();
            switch (method) {
                case "CASH" -> totalCash += t.getTotalAmount();
                case "GCASH" -> totalGCash += t.getTotalAmount();
                case "CREDIT" -> totalCredit += t.getTotalAmount();
                default -> { }
            }
            for (TransactionItem item : t.getItems()) {
                double[] agg = itemAgg.computeIfAbsent(item.getItemName(), k -> new double[2]);
                agg[0] += item.getQuantity();
                agg[1] += item.getSubtotal();
            }
        }

        totalSales = round2(totalSales);
        totalCash = round2(totalCash);
        totalGCash = round2(totalGCash);
        totalCredit = round2(totalCredit);

        MonthlySale summary = monthlySaleRepository.findByBranch_IdAndDay(branchId, day)
                .orElseGet(MonthlySale::new);
        summary.setBranch(branch);
        summary.setDay(day);
        summary.setTotalSales(totalSales);
        summary.setTotalCash(totalCash);
        summary.setTotalGCash(totalGCash);
        summary.setTotalCredit(totalCredit);
        summary.setTransactionCount(transactions.size());
        summary.setItemsBreakdownJson(buildItemsJson(itemAgg));
        summary.setCategoryBreakdownJson("{}");

        MonthlySale saved = monthlySaleRepository.save(summary);
        transactionRepository.deleteAll(transactions);

        return saved;
    }

    @Transactional
    public void resetMonthlySales(Long branchId, YearMonth yearMonth) {
        if (!branchRepository.existsById(branchId)) {
            throw new IllegalStateException("Branch not found: " + branchId);
        }
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        monthlySaleRepository.deleteByBranch_IdAndDayBetween(branchId, start, end);
    }

    /**
     * FIX (race condition): item stock is now read+locked via
     * findByBranch_IdAndBarcodeForUpdate(...) (PESSIMISTIC_WRITE) instead of
     * the plain finder. Two cashiers checking out the same item at nearly
     * the same time used to both read the "before" stock, both pass the
     * check, and one save silently overwrote the other — causing either a
     * false "Insufficient stock" failure or an actual oversell, with no
     * trace of why. Now the second concurrent checkout simply WAITS for the
     * DB row lock to release, then re-reads the already-updated stock, so
     * it either succeeds correctly or fails for a REAL reason — never a
     * phantom one caused by the race itself.
     *
     * FIX (visibility): any failure here — including ones a retry would fix
     * on its own — is now persisted to failed_transactions BEFORE the
     * exception is thrown, so Sales.java's "Failed Transactions" dialog
     * shows the real reason without needing the cashier to relay it.
     *
     * NEW (idempotency): if request.idempotencyKey() matches an already-
     * saved transaction, that SAME transaction is returned immediately —
     * no stock is deducted and no new row is inserted. This makes it safe
     * for the client to retry a checkout it's not sure went through (e.g.
     * after a network timeout or an app crash before the response arrived)
     * using the same key: the retry either creates the transaction for the
     * first time, or discovers it already exists and just returns it.
     *
     * A DataIntegrityViolationException on save (unique constraint on
     * idempotency_key) means two near-simultaneous requests with the same
     * key both passed the findByIdempotencyKey check before either
     * committed — the loser of that race simply looks the winner's row up
     * and returns it instead of failing.
     */
    @Transactional
    public Transaction recordTransaction(TransactionRequest request) {
        // Idempotent replay check — must happen before ANY stock mutation.
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Long branchIdForLogging = null;
        String branchNameForLogging = null;

        try {
            if (request.accountId() == null || request.accountId().isBlank()) {
                throw new IllegalArgumentException("accountId is required.");
            }
            if (request.items() == null || request.items().isEmpty()) {
                throw new IllegalArgumentException("At least one item is required.");
            }
            String method = request.paymentMethod() == null ? "" : request.paymentMethod().toUpperCase();
            if (!method.equals("CASH") && !method.equals("GCASH") && !method.equals("CREDIT")) {
                throw new IllegalArgumentException("paymentMethod must be CASH, GCASH, or CREDIT.");
            }
            if (method.equals("GCASH") && (request.gcashAccountName() == null || request.gcashAccountName().isBlank())) {
                throw new IllegalArgumentException("gcashAccountName is required for GCASH payments.");
            }

            Account account = accountRepository.findByAccountId(request.accountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + request.accountId()));
            Long branchId = account.getBranch().getId();
            branchIdForLogging = branchId;
            branchNameForLogging = account.getBranch().getBranchName();

            Transaction transaction = new Transaction();
            transaction.setAccount(account);
            transaction.setPaymentMethod(method);
            transaction.setGcashAccountName(method.equals("GCASH") ? request.gcashAccountName() : null);
            transaction.setIdempotencyKey(
                    request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                            ? null : request.idempotencyKey());

            BigDecimal totalAmount = BigDecimal.ZERO;
            for (TransactionItemRequest itemReq : request.items()) {
                if (itemReq.quantity() <= 0) {
                    throw new IllegalArgumentException("Quantity must be positive for item: " + itemReq.itemName());
                }

                // FIX: locked read — blocks concurrent checkouts on the SAME
                // item row instead of letting them race each other.
                Item stockItem = itemRepo.findByBranch_IdAndBarcodeForUpdate(branchId, itemReq.barcode())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Item not found for this branch (barcode " + itemReq.barcode() + ")."));

                deductStock(stockItem, itemReq.quantity());

                TransactionItem line = new TransactionItem(
                        itemReq.itemName(),
                        itemReq.unit(),
                        itemReq.price(),
                        itemReq.quantity(),
                        itemReq.barcode()
                );
                transaction.addItem(line);
                totalAmount = totalAmount.add(toMoney(line.getSubtotal()));
            }

            // FIX: BigDecimal comparison — removes float-summation drift
            // between the client's running total and this independently
            // recomputed total.
            BigDecimal tendered = toMoney(request.tenderedAmount());
            if (tendered.compareTo(totalAmount) < 0) {
                throw new IllegalArgumentException("Tendered amount is less than the total amount due.");
            }

            transaction.setTotalAmount(totalAmount.doubleValue());
            transaction.setTenderedAmount(tendered.doubleValue());
            transaction.setChangeAmount(tendered.subtract(totalAmount).doubleValue());
            transaction.setTransactionCode(generateTransactionCode());

            try {
                return transactionRepository.save(transaction);
            } catch (DataIntegrityViolationException dup) {
                // Concurrent retry race on the same idempotencyKey — the
                // other request won, so just return its saved row instead
                // of failing (and instead of deducting stock twice, since
                // this save never committed).
                if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
                    Optional<Transaction> winner = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
                    if (winner.isPresent()) {
                        return winner.get();
                    }
                }
                throw dup;
            }

        } catch (IllegalArgumentException | IllegalStateException ex) {
            logFailedAttempt(request, branchIdForLogging, branchNameForLogging, ex.getMessage());
            throw ex;
        }
    }

    /** Best-effort logging — a failure here must never mask the real error. */
    private void logFailedAttempt(TransactionRequest request, Long branchId, String branchName, String reason) {
        try {
            FailedTransaction failed = new FailedTransaction();
            failed.setAccountId(request.accountId());
            failed.setBranchId(branchId);
            failed.setBranchName(branchName);
            failed.setPaymentMethod(request.paymentMethod());
            failed.setTenderedAmount(request.tenderedAmount());
            failed.setComputedTotal(0.0); // failure may happen before total is known
            failed.setItemsJson(buildRequestItemsJson(request));
            failed.setReason(reason == null ? "Unknown error" : reason);
            failedTransactionRepository.save(failed);
        } catch (Exception loggingFailure) {
            System.out.println("[TransactionService] Could not log failed transaction attempt: "
                    + loggingFailure.getMessage());
        }
    }

    private String buildRequestItemsJson(TransactionRequest request) {
        if (request.items() == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (TransactionItemRequest item : request.items()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"itemName\":\"").append(escapeJson(item.itemName())).append("\",")
              .append("\"quantity\":").append(item.quantity()).append(",")
              .append("\"price\":").append(item.price()).append(",")
              .append("\"barcode\":\"").append(escapeJson(item.barcode())).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    @Transactional
    public void deleteTransaction(String transactionCode) {
        Transaction txn = transactionRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Transaction not found: " + transactionCode
                                + " (it may have already been deleted or archived)."));

        Long branchId = txn.getAccount().getBranch().getId();
        for (TransactionItem line : txn.getItems()) {
            itemRepo.findByBranch_IdAndBarcode(branchId, line.getBarcode()).ifPresent(stockItem -> {
                BigDecimal current = stockItem.getQuantity() == null ? BigDecimal.ZERO : stockItem.getQuantity();
                BigDecimal restored = current.add(BigDecimal.valueOf(line.getQuantity()));
                stockItem.setQuantity(restored.setScale(2, RoundingMode.HALF_UP));
                itemRepo.save(stockItem);
            });
        }

        transactionRepository.delete(txn);
    }

    private void deductStock(Item item, int quantity) {
        BigDecimal current = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
        BigDecimal requested = BigDecimal.valueOf(quantity);
        if (current.compareTo(requested) < 0) {
            throw new IllegalStateException(
                    "Insufficient stock for " + item.getItemName() + ". Available: " + current + ", requested: " + requested);
        }
        item.setQuantity(current.subtract(requested).setScale(2, RoundingMode.HALF_UP));
        itemRepo.save(item);
    }

    public List<Transaction> loadTransactionsForBranchAndDate(Long branchId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return transactionRepository.findByAccount_Branch_IdAndCreatedAtBetweenOrderByCreatedAtAsc(
                branchId, start, end);
    }

    /** Used by the new "Failed Transactions" endpoint in TransactionController. */
    public List<FailedTransaction> loadFailedTransactionsForBranchAndDate(Long branchId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return failedTransactionRepository.findByBranchIdAndAttemptedAtBetweenOrderByAttemptedAtDesc(
                branchId, start, end);
    }

    private String generateTransactionCode() {
        String candidate;
        do {
            candidate = randomCodeCandidate();
        } while (transactionRepository.existsByTransactionCode(candidate));
        return candidate;
    }

    private String randomCodeCandidate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private BigDecimal toMoney(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildItemsJson(Map<String, double[]> itemAgg) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<String, double[]> e : itemAgg.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"itemName\":\"").append(escapeJson(e.getKey())).append("\",")
              .append("\"quantity\":").append(e.getValue()[0]).append(",")
              .append("\"amount\":").append(round2(e.getValue()[1])).append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}