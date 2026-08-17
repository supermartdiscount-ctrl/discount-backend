package Controller;

import Request.RefundRequest;
import Response.RefundResponse;
import function.Refunded;
import function.RefundService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    public ResponseEntity<?> addRefund(@RequestBody RefundRequest request) {
        try {
            Refunded saved = refundService.processRefund(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(RefundResponse.from(saved));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public List<RefundResponse> getRefunds(
            @RequestParam(value = "branchName", required = false) String branchName) {
        return refundService.getRefunds(branchName).stream()
                .map(RefundResponse::from)
                .toList();
    }

    // NEW — DELETE /api/refunds  or  DELETE /api/refunds?branchName=Main Branch
    // Permanently removes Refunded rows. Does NOT touch transactions/stock —
    // it only clears refund history records.
    @DeleteMapping
    public ResponseEntity<?> resetRefunds(
            @RequestParam(value = "branchName", required = false) String branchName) {
        long deletedCount = refundService.resetRefunds(branchName);
        return ResponseEntity.ok(Map.of("deletedCount", deletedCount));
    }
}