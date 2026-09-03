package Repo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row per branch + account, per duty date: the cashier's starting cash
 * fund and that day's sales quota, entered via the "Start of Duty" dialog
 * right after login (Controller.Funds_Quota on the frontend).
 *
 * NOTE: If your project is on Spring Boot 2.x, change the
 * "jakarta.persistence.*" import above to "javax.persistence.*".
 */
@Entity
@Table(name = "funds_quota", uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "account_id", "duty_date"}))
public class FundsQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "duty_date", nullable = false)
    private LocalDate dutyDate;

    @Column(name = "starting_fund", nullable = false)
    private double startingFund;

    @Column(name = "daily_quota", nullable = false)
    private double dailyQuota;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FundsQuota() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public LocalDate getDutyDate() {
        return dutyDate;
    }

    public void setDutyDate(LocalDate dutyDate) {
        this.dutyDate = dutyDate;
    }

    public double getStartingFund() {
        return startingFund;
    }

    public void setStartingFund(double startingFund) {
        this.startingFund = startingFund;
    }

    public double getDailyQuota() {
        return dailyQuota;
    }

    public void setDailyQuota(double dailyQuota) {
        this.dailyQuota = dailyQuota;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}