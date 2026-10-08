package com.kubee.auth.tenant.repository;

import com.kubee.auth.tenant.entity.TenantDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantDetailsRepository extends JpaRepository<TenantDetails, Long> {

    Optional<TenantDetails> findByTenantId(Long tenantId);
}
