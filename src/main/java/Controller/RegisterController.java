package Controller;

import Request.AccountRequest;
import Request.AccountResponse;
import Request.BranchRequest;
import function.Account;
import function.Branch;
import function.Register_Branch_Account;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RegisterController {

    private final Register_Branch_Account registerService;

    public RegisterController(Register_Branch_Account registerService) {
        this.registerService = registerService;
    }

    // POST /api/branches  { "branchName": "North Branch" }
    @PostMapping("/branches")
    public ResponseEntity<?> registerBranch(@RequestBody BranchRequest request) {
        try {
            Branch branch = registerService.registerBranch(request.branchName());
            return ResponseEntity.status(HttpStatus.CREATED).body(branch);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/branches
    @GetMapping("/branches")
    public List<Branch> getAllBranches() {
        return registerService.loadAllBranches();
    }

    // POST /api/accounts  { "branchName": "...", "fullName": "...", "email": "...", "role": "...", "password": "..." }
    @PostMapping("/accounts")
    public ResponseEntity<?> registerAccount(@RequestBody AccountRequest request) {
        try {
            Account account = registerService.registerAccount(
                    request.branchName(), request.fullName(), request.email(),
                    request.role(), request.password()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/branches/{branchName}/accounts
    @GetMapping("/branches/{branchName}/accounts")
    public List<AccountResponse> getAccountsForBranch(@PathVariable("branchName") String branchName) {
        return registerService.loadAccountsForBranch(branchName)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    // DELETE /api/accounts/{accountId}
    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<?> deleteAccount(@PathVariable("accountId") String accountId) {
        try {
            boolean deleted = registerService.deleteAccount(accountId);
            if (deleted) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Account not found: " + accountId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}