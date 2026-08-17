package Response;

import function.MonthlySale;

public record ArchiveResponse(
        String branchName,
        String day,
        double totalSales,
        double totalCash,
        double totalGCash,
        double totalCredit,
        int transactionCount,
        String itemsBreakdownJson
) {
    public static ArchiveResponse from(MonthlySale m) {
        return new ArchiveResponse(
                m.getBranch().getBranchName(),
                m.getDay().toString(),
                m.getTotalSales(),
                m.getTotalCash(),
                m.getTotalGCash(),
                m.getTotalCredit(),
                m.getTransactionCount(),
                m.getItemsBreakdownJson()
        );
    }
}