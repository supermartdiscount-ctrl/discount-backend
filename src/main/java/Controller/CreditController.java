package Controller;

import Request.CreditRequest;
import Response.CreditResponse;
import function.Credit;
import function.CreditSave;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/credits")
@CrossOrigin
public class CreditController {

    private final CreditSave creditSave;

    public CreditController(CreditSave creditSave) {
        this.creditSave = creditSave;
    }

    /** Called from Home.java's Credit checkout dialog (hotkey R). */
    @PostMapping
    public ResponseEntity<?> saveCredit(@RequestBody CreditRequest request) {
        try {
            Credit saved = creditSave.recordCredit(request);
            return ResponseEntity.ok(Map.of(
                    "id", saved.getId(),
                    "transactionCode", saved.getTransactionCode(),
                    "totalAmount", saved.getTotalAmount(),
                    "status", saved.getStatus().name()
            ));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    /** Called from Credit.java to populate its table. */
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<?> getCreditsForBranch(@PathVariable("branchId") Long branchId) {
        try {
            List<CreditResponse> credits = creditSave.loadCreditsForBranch(branchId);
            return ResponseEntity.ok(credits);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    /** Called from Credit.java's "Mark Paid" flow. */
    @PatchMapping("/{creditId}/mark-paid")
    public ResponseEntity<?> markAsPaid(@PathVariable("creditId") Long creditId,
                                         @RequestBody Request.MarkPaidRequest request) {
        try {
            Credit updated = creditSave.markAsPaid(creditId, request.paidVia(),
                    request.tenderedAmount(), request.gcashAccountName());
            return ResponseEntity.ok(Map.of(
                    "id", updated.getId(),
                    "status", updated.getStatus().name(),
                    "paidVia", updated.getPaidVia()
            ));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    /** Called from Credit.java's "Delete" button. */
    @DeleteMapping("/{creditId}")
    public ResponseEntity<?> deleteCredit(@PathVariable("creditId") Long creditId) {
        try {
            creditSave.deleteCredit(creditId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }
}