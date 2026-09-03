package Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface FundsQuotaRepository extends JpaRepository<FundsQuota, Long> {

    // OLD — kept only in case other code still calls it; no longer used
    // by FundsQuotaService once per-account scoping is in place.
    Optional<FundsQuota> findByBranchIdAndDutyDate(Long branchId, LocalDate dutyDate);

    // NEW — scoped per account, so each cashier's starting fund/quota is
    // isolated from every other cashier's, even on the same branch/date.
    Optional<FundsQuota> findByBranchIdAndAccountIdAndDutyDate(Long branchId, String accountId, LocalDate dutyDate);
}