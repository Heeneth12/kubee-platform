package com.kubee.inventory.payment.repository;

import com.kubee.inventory.payment.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

    List<PaymentAllocation> findByInvoiceIdAndTenantIdOrderByAllocationDateDesc(Long invoiceId, Long tenantId);
}
