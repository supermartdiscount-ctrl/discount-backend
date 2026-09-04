package Controller;

import Request.TransactionRequest;
import Response.TransactionResponse;
import function.Monthly;
import function.Transaction;
import function.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import function.MonthlySale;
import Response.ArchiveResponse;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // POST /api/transactions
    // body: { "accountId": "ACC-0001", "paymentMethod": "CASH", "gcashAccountName": null,
    //         "tenderedAmount": 500.0, "items": [ {itemName, unit, price, quantity, barcode}, ... ] }
    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody TransactionRequest request) {
        try {
            Transaction saved = transactionService.recordTransaction(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(saved));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/transactions/branch/{branchId}?date=2026-07-14
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<?> getTransactionsForBranch(
            @PathVariable("branchId") Long branchId,
            @RequestParam("date") String date) {
        try {
            LocalDate parsedDate = LocalDate.parse(date);
            List<TransactionResponse> results = transactionService
                    .loadTransactionsForBranchAndDate(branchId, parsedDate)
                    .stream()
                    .map(TransactionResponse::from)
                    .toList();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date format. Use yyyy-MM-dd."));
        }
    }

    // DELETE /api/transactions/{transactionCode}
    // Permanently deletes a single still-live (un-archived) transaction and
    // restores the stock that was deducted for its line items. Backs the
    // per-row ✕ delete button in Sales.java's Transactions table.
    @DeleteMapping("/{transactionCode}")
    public ResponseEntity<?> deleteTransaction(@PathVariable("transactionCode") String transactionCode) {
        try {
            transactionService.deleteTransaction(transactionCode);
            return ResponseEntity.ok(Map.of("status", "deleted", "transactionCode", transactionCode));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/transactions/branch/{branchId}/monthly?month=2026-07
    // Returns one entry per day that has transactions, with that day's
    // total sales amount for the given branch. Used by the Monthly report.
    @GetMapping("/branch/{branchId}/monthly")
    public ResponseEntity<?> getMonthlySalesForBranch(
            @PathVariable("branchId") Long branchId,
            @RequestParam("month") String month) {
        try {
            YearMonth yearMonth = YearMonth.parse(month);
            List<Monthly> results = transactionService.loadDailyNetSalesForBranchAndMonth(branchId, yearMonth);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid month format. Use yyyy-MM."));
        }
    }

    // DELETE /api/transactions/branch/{branchId}/monthly?month=2026-07
    // Permanently deletes every archived monthly_sales row for this branch
    // in the given month. Backs the "Reset Data" button on Monthly.java.
    // Does NOT touch un-archived live transactions.
    @DeleteMapping("/branch/{branchId}/monthly")
    public ResponseEntity<?> resetMonthlySales(
            @PathVariable("branchId") Long branchId,
            @RequestParam("month") String month) {
        try {
            YearMonth yearMonth = YearMonth.parse(month);
            transactionService.resetMonthlySales(branchId, yearMonth);
            return ResponseEntity.ok(Map.of("status", "deleted", "branchId", branchId, "month", month));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid month format. Use yyyy-MM."));
        }
    }

    // POST /api/transactions/branch/{branchId}/archive?date=2026-07-14
    // Archives that day's totals + item breakdown into monthly_sales, then
    // deletes the day's rows from transactions/transaction_items.
    @PostMapping("/branch/{branchId}/archive")
    public ResponseEntity<?> archiveDay(
            @PathVariable("branchId") Long branchId,
            @RequestParam("date") String date) {
        try {
            LocalDate day = LocalDate.parse(date);
            MonthlySale saved = transactionService.archiveAndClearDay(branchId, day);
            return ResponseEntity.ok(ArchiveResponse.from(saved));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date format. Use yyyy-MM-dd."));
        }
    }
}