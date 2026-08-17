package Request;

import java.util.List;

public record TransactionRequest(
        String accountId,
        String paymentMethod,      // CASH | GCASH | CREDIT
        String gcashAccountName,   // nullable, only meaningful when paymentMethod = GCASH
        double tenderedAmount,
        List<TransactionItemRequest> items
) {
}