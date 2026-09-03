package Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository for Funds_Quota "Start of Duty" records.
 *
 * FIX: lookups are scoped to (branchId, accountId, dutyDate). The old
 * findByBranchIdAndDutyDate(Long, LocalDate) method has been removed —
 * it queried by branch + date only, which is exactly what caused every
 * cashier on the same branch/day to collide on one shared row and, after
 * the entity's unique constraint was widened to include account_id,
 * caused the "could not execute statement" 500 error on save.
 */
public interface FundsQuotaRepository extends JpaRepository<FundsQuota, Long> {

    Optional<FundsQuota> findByBranchIdAndAccountIdAndDutyDate(
            Long branchId, String accountId, LocalDate dutyDate);
}