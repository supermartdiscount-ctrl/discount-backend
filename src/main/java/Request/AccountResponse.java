package Request;

import function.Account;

public record AccountResponse(
        String accountId,
        String fullName,
        String email,
        String role,
        String password,
        String branchName
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getFullName(),
                account.getEmail(),
                account.getRole(),
                account.getPassword(),
                account.getBranch().getBranchName()
        );
    }
}