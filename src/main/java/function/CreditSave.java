package function;

import Repo.AccountRepository;
import Repo.BranchRepository;
import Repo.CreditRepository;
import Repo.ItemRepo;
import Repo.TransactionRepository;
import Request.CreditItemRequest;
import Request.CreditRequest;
import Response.CreditItemResponse;
import Response.CreditResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CreditSave {

    private static final String CODE_CHARS = "0123456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CreditRepository creditRepository;
    private final AccountRepository accountRepository;
    private final ItemRepo itemRepo;
    private final BranchRepository branchRepository;
    private final TransactionRepository transactionRepository;

    public CreditSave(CreditRepository creditRepository,
                       AccountRepository accountRepository,
                       ItemRepo itemRepo,
                       BranchRepository branchRepository,
                       TransactionRepository transactionRepository) {
        this.creditRepository = creditRepository;
        this.accountRepository = accountRepository;
        this.itemRepo = itemRepo;
        this.branchRepository = branchRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Credit recordCredit(CreditRequest request) {
        // ...unchanged...
        if (request.accountId() == null || request.accountId().isBlank()) {
            throw new IllegalArgumentException("accountId is required.");
        }
        if (request.customerName() == null || request.customerName().isBlank()) {
            throw new IllegalArgumentException("customerName is required.");
        }
        if (request.customerContact() == null || request.customerContact().isBlank()) {
            throw new IllegalArgumentException("customerContact is required.");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("At least one item is required.");
        }

        Account account = accountRepository.findByAccountId(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + request.accountId()));
        Long branchId = account.getBranch().getId();

        Credit credit = new Credit();
        credit.setAccount(account);
        credit.setCustomerName(request.customerName().trim());
        credit.setCustomerContact(request.customerContact().trim());
        credit.setCustomerAddress(request.customerAddress());
        credit.setStatus(Credit.Status.UNPAID);

        double totalAmount = 0.0;
        for (CreditItemRequest itemReq : request.items()) {
            if (itemReq.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for item: " + itemReq.itemName());
            }

            Item stockItem = itemRepo.findByBranch_IdAndBarcode(branchId, itemReq.barcode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Item not found for this branch (barcode " + itemReq.barcode() + ")."));

            deductStock(stockItem, itemReq.quantity());

            CreditItem line = new CreditItem(
                    itemReq.itemName(),
                    itemReq.category(),
                    itemReq.unit(),
                    itemReq.price(),
                    itemReq.quantity(),
                    itemReq.barcode()
            );
            credit.addItem(line);
            totalAmount += line.getSubtotal();
        }

        credit.setTotalAmount(round2(totalAmount));
        credit.setTransactionCode(generateCreditCode());

        return creditRepository.save(credit);
    }

    public List<CreditResponse> loadCreditsForBranch(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new IllegalStateException("Branch not found: " + branchId);
        }
        List<Credit> credits = creditRepository.findByAccount_Branch_IdOrderByCreatedAtDesc(branchId);
        List<CreditResponse> results = new ArrayList<>();
        for (Credit c : credits) {
            results.add(toResponse(c));
        }
        return results;
    }

    /**
     * Marks a Credit as PAID and, in the same transaction, records a real
     * Transaction (CASH or GCASH) for it — the same way a normal checkout
     * would — so it shows up in Sales/Monthly reports. The Transaction is
     * stamped with LocalDateTime.now() (its constructor default), i.e. the
     * moment the cashier actually marked it paid, NOT the credit's original
     * createdAt. Stock is NOT deducted again here — it was already deducted
     * when the credit itself was recorded in recordCredit().
     */
    @Transactional
    public Credit markAsPaid(Long creditId, String paidVia, double tenderedAmount, String gcashAccountName) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalStateException("Credit record not found: " + creditId));
        if (credit.getStatus() == Credit.Status.PAID) {
            throw new IllegalStateException("This credit record is already marked as paid.");
        }
        String method = paidVia == null ? "" : paidVia.toUpperCase();
        if (!method.equals("CASH") && !method.equals("GCASH")) {
            throw new IllegalArgumentException("paidVia must be CASH or GCASH.");
        }
        if (method.equals("GCASH") && (gcashAccountName == null || gcashAccountName.isBlank())) {
            throw new IllegalArgumentException("gcashAccountName is required for GCASH payments.");
        }
        if (tenderedAmount < credit.getTotalAmount()) {
            throw new IllegalArgumentException("Tendered amount is less than the amount due.");
        }

        credit.setStatus(Credit.Status.PAID);
        credit.setPaidVia(method);
        credit.setPaidAt(LocalDateTime.now());
        Credit saved = creditRepository.save(credit);

        recordTransactionForPaidCredit(saved, method, tenderedAmount, gcashAccountName);

        return saved;
    }

    /** Builds and saves a Transaction mirroring this credit's items, dated to right now. */
    private void recordTransactionForPaidCredit(Credit credit, String paidVia,
                                                 double tenderedAmount, String gcashAccountName) {
        Transaction transaction = new Transaction(); // createdAt = LocalDateTime.now() via its constructor
        transaction.setAccount(credit.getAccount());
        transaction.setPaymentMethod(paidVia);
        transaction.setGcashAccountName(paidVia.equals("GCASH") ? gcashAccountName : null);

        for (CreditItem ci : credit.getItems()) {
            TransactionItem line = new TransactionItem(
                    ci.getItemName(),
                    ci.getUnit(),
                    ci.getPrice(),
                    ci.getQuantity(),
                    ci.getBarcode()
            );
            transaction.addItem(line);
        }

        transaction.setTotalAmount(credit.getTotalAmount());
        transaction.setTenderedAmount(round2(tenderedAmount));
        transaction.setChangeAmount(round2(tenderedAmount - credit.getTotalAmount()));
        transaction.setTransactionCode(generateTransactionCode());

        transactionRepository.save(transaction);
    }

    @Transactional
    public void deleteCredit(Long creditId) {
        if (!creditRepository.existsById(creditId)) {
            throw new IllegalStateException("Credit record not found: " + creditId);
        }
        creditRepository.deleteById(creditId);
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

    private String generateCreditCode() {
        String candidate;
        do {
            candidate = randomCodeCandidate();
        } while (creditRepository.existsByTransactionCode(candidate));
        return candidate;
    }

    /** Separate uniqueness check against TransactionRepository — credit codes and transaction codes are different tables/sequences. */
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

    private CreditResponse toResponse(Credit c) {
        List<CreditItemResponse> items = new ArrayList<>();
        for (CreditItem item : c.getItems()) {
            items.add(new CreditItemResponse(
                    item.getBarcode(),
                    item.getItemName(),
                    item.getCategory(),
                    item.getUnit(),
                    item.getPrice(),
                    item.getQuantity(),
                    item.getSubtotal()
            ));
        }
        return new CreditResponse(
                c.getId(),
                c.getTransactionCode(),
                c.getCustomerName(),
                c.getCustomerContact(),
                c.getCustomerAddress(),
                c.getAccount().getAccountId(),
                c.getTotalAmount(),
                c.getStatus().name(),
                c.getPaidVia(),
                c.getCreatedAt(),
                c.getPaidAt(),
                items
        );
    }
}