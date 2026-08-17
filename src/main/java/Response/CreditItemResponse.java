package Response;

public record CreditItemResponse(
        String barcode,
        String itemName,
        String category,
        String unit,
        double price,
        int quantity,
        double subtotal
) {}