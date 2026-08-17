package Repo;

import function.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountId(String accountId);
    Optional<Account> findByAccountId(String accountId);
    List<Account> findByBranch_BranchName(String branchName);
    long deleteByAccountId(String accountId);
}