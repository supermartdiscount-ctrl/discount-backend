package Request;

public record RefundRequest(
        String transactionCode,
        String barcode,
        String itemName,
        String unit,
        int quantity,
        double sellingPrice,     // fallback only - server prefers the transaction line's actual price
        String refundMethod,     // CASH | GCASH | STORE CREDIT
        String customerName,
        String receiptDate       // yyyy-MM-dd
) {}