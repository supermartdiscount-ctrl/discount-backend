package Request;

import java.util.List;

/**
 * idempotencyKey (NEW): a UUID generated once on the client per checkout
 * attempt (Home.java's completeSale()). Any retry of the SAME attempt
 * (after a timeout/dropped connection) reuses this exact key so
 * TransactionService.recordTransaction() can recognize and safely dedupe
 * it instead of saving/deducting stock twice. Nullable for backward
 * compatibility with any older client build.
 */
public record TransactionRequest(
        String accountId,
        String paymentMethod,      // CASH | GCASH | CREDIT
        String gcashAccountName,   // nullable, only meaningful when paymentMethod = GCASH
        double tenderedAmount,
        String idempotencyKey,     // NEW
        List<TransactionItemRequest> items
) {
}