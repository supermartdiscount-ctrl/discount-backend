package Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface FundsQuotaRepository extends JpaRepository<FundsQuota, Long> {

    Optional<FundsQuota> findByBranchIdAndDutyDate(Long branchId, LocalDate dutyDate);
}