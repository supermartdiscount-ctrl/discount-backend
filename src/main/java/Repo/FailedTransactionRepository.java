package Repo;

import function.FailedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FailedTransactionRepository extends JpaRepository<FailedTransaction, Long> {

    // branchId can be null (e.g. failure happened before the account's
    // branch could even be resolved), so this is branch-scoped only when
    // branchId is known — see the service method for how it's called.
    List<FailedTransaction> findByBranchIdAndAttemptedAtBetweenOrderByAttemptedAtDesc(
            Long branchId, LocalDateTime start, LocalDateTime end);
}