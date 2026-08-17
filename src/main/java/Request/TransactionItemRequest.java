package Request;

public record TransactionItemRequest(
        String itemName,
        String unit,
        double price,
        int quantity,
        String barcode
) {
}