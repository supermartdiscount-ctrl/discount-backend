package Request;

/** Body for PATCH /api/credits/{id}/mark-paid. */
public record MarkPaidRequest(
        String paidVia,          // CASH | GCASH
        double tenderedAmount,   // amount the customer actually handed over (Cash) or the exact total (GCash)
        String gcashAccountName  // only meaningful when paidVia = GCASH
) {}