package Repo;

import function.Credit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditRepository extends JpaRepository<Credit, Long> {
    List<Credit> findByAccount_Branch_IdOrderByCreatedAtDesc(Long branchId);
    boolean existsByTransactionCode(String transactionCode);
}