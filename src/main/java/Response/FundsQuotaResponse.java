package Response;

public class FundsQuotaResponse {

    private Long id;
    private Long branchId;
    private String accountId;
    private String dutyDate;
    private double startingFund;
    private double dailyQuota;

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

    public String getDutyDate() {
        return dutyDate;
    }

    public void setDutyDate(String dutyDate) {
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
}