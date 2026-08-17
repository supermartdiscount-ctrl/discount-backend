package function;

import Repo.ItemRepo;
import Repo.RefundedRepository;
import Repo.TransactionRepository;
import Request.RefundRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class RefundService {

    private final TransactionRepository transactionRepository;
    private final ItemRepo itemRepo;
    private final RefundedRepository refundedRepository;

    public RefundService(TransactionRepository transactionRepository,
                          ItemRepo itemRepo,
                          RefundedRepository refundedRepository) {
        this.transactionRepository = transactionRepository;
        this.itemRepo = itemRepo;
        this.refundedRepository = refundedRepository;
    }

    /**
     * Refunds `quantity` units of `barcode` off transaction `transactionCode`.
     * - Deducts the quantity from that transaction's line (removing the line,
     *   or the whole transaction if nothing is left) and reduces totalAmount.
     * - Adds the quantity back to that branch's Item stock.
     * - Records a Refunded row under the transaction's own account/branch.
     */
    @Transactional
    public Refunded processRefund(RefundRequest request) {
        if (request.transactionCode() == null || request.transactionCode().isBlank()) {
            throw new IllegalArgumentException("Transaction ID is required.");
        }
        if (request.barcode() == null || request.barcode().isBlank()) {
            throw new IllegalArgumentException("Barcode is required.");
        }
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("Refund quantity must be greater than zero.");
        }

        Transaction transaction = transactionRepository.findByTransactionCode(request.transactionCode().trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction not found: " + request.transactionCode()));

        TransactionItem targetLine = transaction.getItems().stream()
                .filter(it -> it.getBarcode() != null && it.getBarcode().equalsIgnoreCase(request.barcode().trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item with barcode " + request.barcode() + " was not found on transaction "
                                + request.transactionCode() + "."));

        if (request.quantity() > targetLine.getQuantity()) {
            throw new IllegalStateException("Cannot refund " + request.quantity()
                    + " - only " + targetLine.getQuantity() + " were sold on this transaction.");
        }

        Branch branch = transaction.getAccount().getBranch();
        Account account = transaction.getAccount();

        double unitPrice = targetLine.getPrice(); // trust the receipt's actual price, not client input
        double refundAmount = round2(unitPrice * request.quantity());
        String itemName = targetLine.getItemName();
        String unit = targetLine.getUnit();

        // 1) Reduce/remove the sold quantity on the live transaction
        int remaining = targetLine.getQuantity() - request.quantity();
        if (remaining <= 0) {
            transaction.getItems().remove(targetLine);
        } else {
            targetLine.setQuantity(remaining);
        }
        transaction.setTotalAmount(round2(transaction.getTotalAmount() - refundAmount));

        if (transaction.getItems().isEmpty()) {
            transactionRepository.delete(transaction);
        } else {
            transactionRepository.save(transaction);
        }

        // 2) Return the refunded quantity to stock (if the item still exists in the catalog)
        Optional<Item> stockItemOpt = itemRepo.findByBranch_IdAndBarcode(branch.getId(), request.barcode().trim());
        stockItemOpt.ifPresent(stockItem -> {
            BigDecimal current = stockItem.getQuantity() == null ? BigDecimal.ZERO : stockItem.getQuantity();
            stockItem.setQuantity(current.add(BigDecimal.valueOf(request.quantity()))
                    .setScale(2, RoundingMode.HALF_UP));
            itemRepo.save(stockItem);
        });

        // 3) Record the refund under the transaction's own account
        Refunded refunded = new Refunded();
        refunded.setTransactionCode(request.transactionCode().trim());
        refunded.setBranch(branch);
        refunded.setAccount(account);
        refunded.setBarcode(request.barcode().trim());
        refunded.setItemName(itemName);
        refunded.setUnit(unit);
        refunded.setQuantity(request.quantity());
        refunded.setSellingPrice(unitPrice);
        refunded.setRefundAmount(refundAmount);
        refunded.setRefundMethod(request.refundMethod());
        refunded.setCustomerName(request.customerName());
        refunded.setReceiptDate(parseReceiptDate(request.receiptDate()));

        return refundedRepository.save(refunded);
    }

    private LocalDate parseReceiptDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
    
    public java.util.List<Refunded> getRefunds(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            return refundedRepository.findAllByOrderByCreatedAtDesc();
        }
        return refundedRepository.findByBranch_BranchNameOrderByCreatedAtDesc(branchName.trim());
    }
    @Transactional
    public long resetRefunds(String branchName) {
        long count;
        if (branchName == null || branchName.isBlank()) {
            count = refundedRepository.count();
            refundedRepository.deleteAll();
        } else {
            List<Refunded> toDelete = refundedRepository.findByBranch_BranchNameOrderByCreatedAtDesc(branchName.trim());
            count = toDelete.size();
            refundedRepository.deleteByBranch_BranchName(branchName.trim());
        }
        return count;
    }
}