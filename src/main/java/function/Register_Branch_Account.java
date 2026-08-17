package function;

import Repo.AccountRepository;
import Repo.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * Spring Boot / MySQL replacement for the old Firebase-based
 * Register_Backend + LocalBackupManager combo.
 *
 * MySQL is now the single source of truth (via Spring Data JPA), so there
 * is no separate "local backup" step anymore - a save either succeeds
 * or throws, same as any normal database write.
 */
@Service
public class Register_Branch_Account {

    private static final String ID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ID_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BranchRepository branchRepository;
    private final AccountRepository accountRepository;

    public Register_Branch_Account(BranchRepository branchRepository, AccountRepository accountRepository) {
        this.branchRepository = branchRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Registers a new branch.
     */
    @Transactional
    public Branch registerBranch(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name is required.");
        }
        String trimmed = branchName.trim();
        if (branchRepository.existsByBranchName(trimmed)) {
            throw new IllegalStateException("Branch already exists: " + trimmed);
        }
        return branchRepository.save(new Branch(trimmed));
    }

    /**
     * Loads all branches.
     */
    public List<Branch> loadAllBranches() {
        return branchRepository.findAll();
    }

    /**
     * Checks if a branch exists.
     */
    public boolean branchExists(String branchName) {
        return branchRepository.existsByBranchName(branchName);
    }

    /**
     * Registers a new account under an existing branch.
     * Auto-generates a unique 10-character accountID, same scheme as before.
     */
    @Transactional
    public Account registerAccount(String branchName, String fullName, String email, String role, String password) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        Branch branch = branchRepository.findByBranchName(branchName)
                .orElseThrow(() -> new IllegalStateException("Branch not found: " + branchName));

        Account account = new Account();
        account.setAccountId(generateAccountID());
        account.setFullName(fullName.trim());
        account.setEmail(email.trim());
        account.setRole(role);
        account.setPassword(password);
        account.setBranch(branch);

        return accountRepository.save(account);
    }

    /**
     * Loads all accounts for a given branch.
     */
    public List<Account> loadAccountsForBranch(String branchName) {
        return accountRepository.findByBranch_BranchName(branchName);
    }

    /**
     * Deletes the account with the given accountId.
     *
     * @return true if an account was actually deleted, false if none matched
     */
    @Transactional
    public boolean deleteAccount(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID is required.");
        }
        return accountRepository.deleteByAccountId(accountId) > 0;
    }

    /**
     * Generates a unique account ID, retrying on collision.
     */
    private String generateAccountID() {
        int maxAttempts = 10;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String candidate = randomIdCandidate();
            if (!accountRepository.existsByAccountId(candidate)) {
                return candidate;
            }
        }
        // Extremely unlikely fallback - append timestamp to guarantee uniqueness.
        return randomIdCandidate() + (System.currentTimeMillis() % 1000);
    }

    private String randomIdCandidate() {
        StringBuilder sb = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            sb.append(ID_CHARS.charAt(RANDOM.nextInt(ID_CHARS.length())));
        }
        return sb.toString();
    }
}