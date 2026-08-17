package function;

import Repo.AccountRepository;
import Repo.BranchRepository;
import Repo.ItemRepo;
import Repo.MonthlySaleRepository;
import Repo.TransactionRepository;
import Request.TransactionItemRequest;
import Request.TransactionRequest;
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

    public TransactionService(TransactionRepository transactionRepository,
                               AccountRepository accountRepository,
                               ItemRepo itemRepo,
                               BranchRepository branchRepository,
                               MonthlySaleRepository monthlySaleRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.itemRepo = itemRepo;
        this.branchRepository = branchRepository;
        this.monthlySaleRepository = monthlySaleRepository;
    }

    // ... recordTransaction(...), deductStock(...), loadTransactionsForBranchAndDate(...),
    //     generateTransactionCode(...), randomCodeCandidate() all stay exactly as you have them ...

    /**
     * Reads the archived monthly_sales table (NOT the live transactions
     * table) so this reflects only days that have been archived via
     * archiveAndClearDay(...). Un-archived days for the current month
     * simply won't appear here yet.
     */
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
    
    /**
     * Archives a branch's sales for one day: aggregates totals + an
     * item-level breakdown from the live transactions/transaction_items
     * rows into monthly_sales, then deletes those live rows. If a
     * monthly_sales row already exists for this branch/day, it is
     * overwritten (so re-archiving the same day is safe/idempotent as
     * long as new transactions came in since the last archive).
     *
     * Throws IllegalStateException if the branch doesn't exist or there
     * is nothing to archive for that day.
     */
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
        Map<String, double[]> itemAgg = new LinkedHashMap<>(); // itemName -> [quantity, amount]

        for (Transaction t : transactions) {
            totalSales += t.getTotalAmount();
            String method = t.getPaymentMethod() == null ? "" : t.getPaymentMethod().toUpperCase();
            switch (method) {
                case "CASH" -> totalCash += t.getTotalAmount();
                case "GCASH" -> totalGCash += t.getTotalAmount();
                case "CREDIT" -> totalCredit += t.getTotalAmount();
                default -> { /* unknown method, still counted in totalSales */ }
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
        // Category breakdown isn't tracked at the item level yet (TransactionItem
        // has no category column) — left empty until that's added.
        summary.setCategoryBreakdownJson("{}");

        MonthlySale saved = monthlySaleRepository.save(summary);

        // Deleting the Transaction entities cascades to TransactionItem
        // (CascadeType.ALL + orphanRemoval on Transaction#items).
        transactionRepository.deleteAll(transactions);

        return saved;
    }
    
    /**
     * Deletes all archived monthly_sales rows for a branch within the
     * given month. This is what backs the "Reset Data" button on the
     * Monthly report screen. It only touches the monthly_sales archive —
     * it has no effect on any live, un-archived transactions for the
     * current month (those live in the transactions table, not here).
     *
     * This is a hard delete with no undo, by design (the UI confirms with
     * the user before calling it).
     */
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
     * Archives a branch's sales for one day: aggregates totals + an
     * item-level breakdown from the live transactions/transaction_items
     * rows into monthly_sales, then deletes those live rows. If a
     * monthly_sales row already exists for this branch/day, it is
     * overwritten (so re-archiving the same day is safe/idempotent as
     * long as new transactions came in since the last archive).
     *
     * Throws IllegalStateException if the branch doesn't exist or there
     * is nothing to archive for that day.
     */
    @Transactional
    public Transaction recordTransaction(TransactionRequest request) {
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

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setPaymentMethod(method);
        transaction.setGcashAccountName(method.equals("GCASH") ? request.gcashAccountName() : null);

        double totalAmount = 0.0;
        for (TransactionItemRequest itemReq : request.items()) {
            if (itemReq.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for item: " + itemReq.itemName());
            }

            Item stockItem = itemRepo.findByBranch_IdAndBarcode(branchId, itemReq.barcode())
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
            totalAmount += line.getSubtotal();
        }

        totalAmount = round2(totalAmount);
        if (request.tenderedAmount() < totalAmount) {
            throw new IllegalArgumentException("Tendered amount is less than the total amount due.");
        }

        transaction.setTotalAmount(totalAmount);
        transaction.setTenderedAmount(request.tenderedAmount());
        transaction.setChangeAmount(round2(request.tenderedAmount() - totalAmount));
        transaction.setTransactionCode(generateTransactionCode());

        return transactionRepository.save(transaction);
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