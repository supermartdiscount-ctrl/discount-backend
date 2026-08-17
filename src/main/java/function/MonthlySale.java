package function;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One archived day's aggregated sales for a branch. Created by
 * TransactionService#archiveAndClearDay when the cashier confirms
 * "Open Monthly Report" on the Sales screen. Once a day is archived here,
 * its raw rows in transactions/transaction_items are deleted.
 */
@Entity
@Table(name = "monthly_sales", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"branch_id", "sale_day"})
})
public class MonthlySale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "sale_day", nullable = false)
    private LocalDate day;

    @Column(name = "total_sales", nullable = false)
    private double totalSales;

    @Column(name = "total_cash", nullable = false)
    private double totalCash;

    @Column(name = "total_gcash", nullable = false)
    private double totalGCash;

    @Column(name = "total_credit", nullable = false)
    private double totalCredit;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount;

    // JSON array, e.g. [{"itemName":"Pancit Canton","quantity":6,"amount":90.0}, ...]
    @Lob
    @Column(name = "items_breakdown_json")
    private String itemsBreakdownJson;

    // Reserved for future use once items carry a category field.
    @Lob
    @Column(name = "category_breakdown_json")
    private String categoryBreakdownJson;

    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;

    public MonthlySale() {
        this.archivedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public LocalDate getDay() { return day; }
    public void setDay(LocalDate day) { this.day = day; }

    public double getTotalSales() { return totalSales; }
    public void setTotalSales(double totalSales) { this.totalSales = totalSales; }

    public double getTotalCash() { return totalCash; }
    public void setTotalCash(double totalCash) { this.totalCash = totalCash; }

    public double getTotalGCash() { return totalGCash; }
    public void setTotalGCash(double totalGCash) { this.totalGCash = totalGCash; }

    public double getTotalCredit() { return totalCredit; }
    public void setTotalCredit(double totalCredit) { this.totalCredit = totalCredit; }

    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }

    public String getItemsBreakdownJson() { return itemsBreakdownJson; }
    public void setItemsBreakdownJson(String itemsBreakdownJson) { this.itemsBreakdownJson = itemsBreakdownJson; }

    public String getCategoryBreakdownJson() { return categoryBreakdownJson; }
    public void setCategoryBreakdownJson(String categoryBreakdownJson) { this.categoryBreakdownJson = categoryBreakdownJson; }

    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
}