package function;

/**
 * One day's aggregated sales for a branch, sourced from the monthly_sales
 * archive table. Serialized to JSON by TransactionController's monthly
 * endpoint and consumed by the frontend's Monthly.java screen.
 */
public class Monthly {

    private String day;
    private double totalAmount;
    private double totalCash;
    private double totalGCash;
    private double totalCredit;
    private int transactionCount;
    private String itemsBreakdownJson;

    public Monthly() {
    }

    public Monthly(String day, double totalAmount, double totalCash, double totalGCash,
                    double totalCredit, int transactionCount, String itemsBreakdownJson) {
        this.day = day;
        this.totalAmount = totalAmount;
        this.totalCash = totalCash;
        this.totalGCash = totalGCash;
        this.totalCredit = totalCredit;
        this.transactionCount = transactionCount;
        this.itemsBreakdownJson = itemsBreakdownJson;
    }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

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
}