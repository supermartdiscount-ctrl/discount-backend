package Response;

import function.Refunded;
import java.time.format.DateTimeFormatter;

public record RefundResponse(
        Long id,
        String transactionCode,
        String barcode,
        String itemName,
        String unit,
        int quantity,
        double sellingPrice,
        double refundAmount,
        String refundMethod,
        String customerName,
        String receiptDate,
        String branchName,
        String accountId,
        String createdAt
) {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");

    public static RefundResponse from(Refunded r) {
        return new RefundResponse(
                r.getId(),
                r.getTransactionCode(),
                r.getBarcode(),
                r.getItemName(),
                r.getUnit(),
                r.getQuantity(),
                r.getSellingPrice(),
                r.getRefundAmount(),
                r.getRefundMethod(),
                r.getCustomerName(),
                r.getReceiptDate() != null ? r.getReceiptDate().format(DATE_FMT) : null,
                r.getBranch().getBranchName(),
                r.getAccount().getAccountId(),
                r.getCreatedAt().format(TS_FMT)
        );
    }
}