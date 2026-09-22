package Repo;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import function.Item;

public interface ItemRepo extends JpaRepository<Item, Long> {

    List<Item> findByBranch_Id(Long branchId);

    Optional<Item> findByBranch_IdAndBarcode(Long branchId, String barcode);

    boolean existsByBranch_IdAndBarcode(Long branchId, String barcode);

    @Query("SELECT i FROM Item i WHERE i.branch.id = :branchId " +
           "AND (LOWER(i.itemName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(i.barcode) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY i.itemName ASC")
    List<Item> searchByBranchAndQuery(@Param("branchId") Long branchId, @Param("query") String query);

    /**
     * Same lookup as findByBranch_IdAndBarcode(...), but takes a
     * PESSIMISTIC_WRITE row lock on the matching Item for the lifetime of
     * the caller's @Transactional method. Used by
     * TransactionService.recordTransaction() so two concurrent checkouts
     * on the same item can never both read the same "before" stock value —
     * the second one waits for the first to commit, then reads the
     * up-to-date quantity. This is the actual fix for transactions that
     * intermittently fail to save but succeed on retry.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.branch.id = :branchId AND i.barcode = :barcode")
    Optional<Item> findByBranch_IdAndBarcodeForUpdate(
            @Param("branchId") Long branchId, @Param("barcode") String barcode);
}