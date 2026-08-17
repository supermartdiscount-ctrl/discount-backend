package Request;

public record AccountRequest(
        String branchName,
        String fullName,
        String email,
        String role,
        String password
) {
}