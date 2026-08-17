package Response;

import function.Transaction;
import function.TransactionItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shaped to map directly onto Sales.java's transaction table columns:
 * {ID, Time, Items, Units, Amount, Payment}.
 */
public record TransactionResponse(
        String transactionCode,
        String time,
        String itemsSummary,
        String unitsSummary,
        double totalAmount,
        double tenderedAmount,
        double changeAmount,
        String paymentMethod,   // e.g. "CASH", "GCASH - Menandro Abalos", "CREDIT"
        String accountId,
        String branchName,
        LocalDateTime createdAt
) {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm:ss a");

    public static TransactionResponse from(Transaction txn) {
        List<TransactionItem> items = txn.getItems();

        String itemsSummary = items.stream()
                .map(TransactionItem::getItemName)
                .collect(Collectors.joining(", "));

        String unitsSummary = items.stream()
                .map(i -> i.getQuantity() + " " + (i.getUnit() == null ? "pc" : i.getUnit()))
                .collect(Collectors.joining(", "));

        String paymentLabel = txn.getPaymentMethod();
        if ("GCASH".equalsIgnoreCase(txn.getPaymentMethod()) && txn.getGcashAccountName() != null) {
            paymentLabel = "GCASH - " + txn.getGcashAccountName();
        }

        return new TransactionResponse(
                txn.getTransactionCode(),
                txn.getCreatedAt().format(TIME_FMT),
                itemsSummary,
                unitsSummary,
                txn.getTotalAmount(),
                txn.getTenderedAmount(),
                txn.getChangeAmount(),
                paymentLabel,
                txn.getAccount().getAccountId(),
                txn.getAccount().getBranch().getBranchName(),
                txn.getCreatedAt()
        );
    }
}