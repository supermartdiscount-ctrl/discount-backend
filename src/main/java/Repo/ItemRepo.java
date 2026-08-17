package Repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
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
}