package Repo;

import function.Refunded;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RefundedRepository extends JpaRepository<Refunded, Long> {
    List<Refunded> findByBranch_IdAndReceiptDateBetweenOrderByCreatedAtDesc(
            Long branchId, LocalDate start, LocalDate end);
    List<Refunded> findByTransactionCode(String transactionCode);

    List<Refunded> findAllByOrderByCreatedAtDesc();
    List<Refunded> findByBranch_BranchNameOrderByCreatedAtDesc(String branchName);

    // NEW — used by the Reset button to permanently delete refund rows
    void deleteByBranch_BranchName(String branchName);
    // deleteAll() is already provided by JpaRepository
}