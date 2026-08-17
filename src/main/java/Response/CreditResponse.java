package Response;

import java.time.LocalDateTime;
import java.util.List;

/** What GET /api/credits/branch/{branchId} returns, one per credit row. */
public record CreditResponse(
        Long id,
        String transactionCode,
        String customerName,
        String customerContact,
        String customerAddress,
        String cashierAccountId,
        double totalAmount,
        String status,   // UNPAID | PAID
        String paidVia,  // null until PAID
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        List<CreditItemResponse> items
) {}