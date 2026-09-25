package Repo;

import function.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByTransactionCode(String transactionCode);

    Optional<Transaction> findByTransactionCode(String transactionCode);

    // account.branch.id -> lets us query "all transactions for this branch"
    // without needing a separate branch_id column on Transaction itself.
    List<Transaction> findByAccount_Branch_IdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long branchId, LocalDateTime start, LocalDateTime end);

    // NEW — backs the idempotent-replay check in
    // TransactionService.recordTransaction(): if a checkout attempt with
    // this exact key was already saved (e.g. the client retried after a
    // timeout), we return that existing row instead of saving/deducting
    // stock again.
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

}