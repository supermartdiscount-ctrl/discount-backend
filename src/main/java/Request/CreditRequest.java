package Request;

import java.util.List;

/**
 * Body for POST /api/credits. accountId identifies the cashier (same
 * accountId used for TransactionRequest), so the branch is resolved the
 * same way: account.getBranch().
 */
public record CreditRequest(
        String accountId,
        String customerName,
        String customerContact,
        String customerAddress,
        List<CreditItemRequest> items
) {}