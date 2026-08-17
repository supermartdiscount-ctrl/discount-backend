package function;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single completed sale (one checkout in Home.java's cart). Always tied to
 * the Account that processed it, which in turn is tied to a Branch - so a
 * transaction's branch is reached via transaction.getAccount().getBranch().
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_code", nullable = false, unique = true, length = 20)
    private String transactionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "payment_method", nullable = false, length = 40)
    private String paymentMethod; // CASH | GCASH | CREDIT

    @Column(name = "gcash_account_name", length = 150)
    private String gcashAccountName; // set only when paymentMethod = GCASH

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Column(name = "tendered_amount", nullable = false)
    private double tenderedAmount;

    @Column(name = "change_amount", nullable = false)
    private double changeAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TransactionItem> items = new ArrayList<>();

    public Transaction() {
        this.createdAt = LocalDateTime.now();
    }

    public void addItem(TransactionItem item) {
        item.setTransaction(this);
        this.items.add(item);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getGcashAccountName() { return gcashAccountName; }
    public void setGcashAccountName(String gcashAccountName) { this.gcashAccountName = gcashAccountName; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getTenderedAmount() { return tenderedAmount; }
    public void setTenderedAmount(double tenderedAmount) { this.tenderedAmount = tenderedAmount; }

    public double getChangeAmount() { return changeAmount; }
    public void setChangeAmount(double changeAmount) { this.changeAmount = changeAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<TransactionItem> getItems() { return items; }
    public void setItems(List<TransactionItem> items) { this.items = items; }
}