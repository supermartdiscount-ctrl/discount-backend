package Request;

public record CreditItemRequest(
        String barcode,
        String itemName,
        String category,
        String unit,
        double price,
        int quantity
) {}