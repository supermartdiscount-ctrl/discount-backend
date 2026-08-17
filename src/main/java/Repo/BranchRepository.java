package Repo;

import function.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByBranchName(String branchName);

    boolean existsByBranchName(String branchName);
}