package function;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Records a checkout attempt that FAILED to save as a real Transaction, so
 * admins can see (via Sales.java's "Failed Transactions" dialog) exactly
 * what happened, when, for which cashier — instead of relying on a
 * cashier's secondhand description of the error.
 *
 * Written from TransactionService.recordTransaction() right before it
 * throws, so it captures the same data the cashier's screen showed at the
 * moment of failure. Intentionally has NO foreign key to Account/Branch —
 * the failure might be caused by exactly one of those being invalid/missing,
 * so we store the raw ids/names as plain columns instead.
 */
@Entity
@Table(name = "failed_transactions")
public class FailedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", length = 50)
    private String accountId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "branch_name", length = 150)
    private String branchName;

    @Column(name = "payment_method", length = 40)
    private String paymentMethod;

    @Column(name = "tendered_amount")
    private double tenderedAmount;

    @Column(name = "computed_total")
    private double computedTotal; // 0.0 if it failed before the total could even be computed

    @Lob
    @Column(name = "items_json")
    private String itemsJson; // raw snapshot of what was in the cart, so nothing is lost

    @Column(name = "reason", length = 500, nullable = false)
    private String reason; // the exact exception message

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    public FailedTransaction() {
        this.attemptedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public double getTenderedAmount() { return tenderedAmount; }
    public void setTenderedAmount(double tenderedAmount) { this.tenderedAmount = tenderedAmount; }

    public double getComputedTotal() { return computedTotal; }
    public void setComputedTotal(double computedTotal) { this.computedTotal = computedTotal; }

    public String getItemsJson() { return itemsJson; }
    public void setItemsJson(String itemsJson) { this.itemsJson = itemsJson; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(LocalDateTime attemptedAt) { this.attemptedAt = attemptedAt; }
}