package Repo;

import function.MonthlySale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MonthlySaleRepository extends JpaRepository<MonthlySale, Long> {
    Optional<MonthlySale> findByBranch_IdAndDay(Long branchId, LocalDate day);
    List<MonthlySale> findByBranch_IdAndDayBetweenOrderByDayAsc(Long branchId, LocalDate start, LocalDate end);
    boolean existsByBranch_IdAndDay(Long branchId, LocalDate day);

    // Used by TransactionService#resetMonthlySales to delete every archived
    // day for a branch within a given month (inclusive range).
    void deleteByBranch_IdAndDayBetween(Long branchId, LocalDate start, LocalDate end);
}