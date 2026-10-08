package com.kubee.auth.branch.repository;

import com.kubee.auth.branch.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findByTenantId(Long tenantId);

    List<Branch> findByTenantIdAndIsActive(Long tenantId, Boolean isActive);

    Optional<Branch> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByBranchCodeAndTenantId(String branchCode, Long tenantId);

    Optional<Branch> findByTenantIdAndIsHeadOfficeTrue(Long tenantId);
}
