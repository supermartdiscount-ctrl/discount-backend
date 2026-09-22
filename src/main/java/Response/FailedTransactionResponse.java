package Response;

import function.FailedTransaction;

import java.time.format.DateTimeFormatter;

public record FailedTransactionResponse(
        String time,
        String accountId,
        String branchName,
        String paymentMethod,
        double tenderedAmount,
        String itemsJson,
        String reason
) {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM dd, hh:mm:ss a");

    public static FailedTransactionResponse from(FailedTransaction f) {
        return new FailedTransactionResponse(
                f.getAttemptedAt().format(TIME_FMT),
                f.getAccountId(),
                f.getBranchName(),
                f.getPaymentMethod(),
                f.getTenderedAmount(),
                f.getItemsJson(),
                f.getReason()
        );
    }
}