package com.kubee.inventory.approval.repository;

import com.kubee.inventory.approval.entity.ApprovalConfig;
import com.kubee.inventory.approval.entity.ApprovalType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalConfigRepository extends JpaRepository<ApprovalConfig, Long> {
    Optional<ApprovalConfig> findByTenantIdAndApprovalType(Long tenantId, ApprovalType approvalType);
    Optional<ApprovalConfig> findByIdAndTenantId(Long id, Long TenantId);
    Optional<ApprovalConfig> findByApprovalTypeAndTenantId(ApprovalType approvalType, Long TenantId);
    Page<ApprovalConfig> findByTenantId(Long TenantId, Pageable pageable);
}