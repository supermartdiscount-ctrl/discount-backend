package Controller;

import function.FundsQuotaService;
import Repo.FundsQuota;
import Request.FundsQuotaRequest;
import Response.FundsQuotaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/funds-quota")
@CrossOrigin(origins = "*")
public class FundsQuotaController {

    private final FundsQuotaService fundsQuotaService;

    public FundsQuotaController(FundsQuotaService fundsQuotaService) {
        this.fundsQuotaService = fundsQuotaService;
    }

    @PostMapping
    public ResponseEntity<?> saveOrUpdate(@RequestBody FundsQuotaRequest request) {
        try {
            FundsQuota saved = fundsQuotaService.saveOrUpdate(
                    request.getBranchId(),
                    request.getAccountId(),
                    request.getDutyDate(),
                    request.getStartingFund(),
                    request.getDailyQuota());
            return ResponseEntity.ok(toResponse(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Failed to save funds/quota: " + e.getMessage());
        }
    }

    /**
     * FIX: endpoint now requires accountId in the path too — GET
     * /api/funds-quota/{branchId}/{accountId}/{date} — so each cashier only
     * ever reads back their own starting fund/quota row.
     */
    @GetMapping("/{branchId}/{accountId}/{date}")
    public ResponseEntity<?> getForBranchAndAccountAndDate(
            @PathVariable("branchId") Long branchId,
            @PathVariable("accountId") String accountId,
            @PathVariable("date") String date) {
        try {
            return fundsQuotaService.getForBranchAndAccountAndDate(branchId, accountId, date)
                    .map(entity -> ResponseEntity.ok(toResponse(entity)))
                    .orElse(ResponseEntity.ok(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Failed to fetch funds/quota: " + e.getMessage());
        }
    }

    private FundsQuotaResponse toResponse(FundsQuota entity) {
        FundsQuotaResponse response = new FundsQuotaResponse();
        response.setId(entity.getId());
        response.setBranchId(entity.getBranchId());
        response.setAccountId(entity.getAccountId());
        response.setDutyDate(entity.getDutyDate().toString());
        response.setStartingFund(entity.getStartingFund());
        response.setDailyQuota(entity.getDailyQuota());
        return response;
    }
}