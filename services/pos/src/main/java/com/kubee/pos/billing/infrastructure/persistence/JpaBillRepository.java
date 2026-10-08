package com.kubee.pos.billing.infrastructure.persistence;

import com.kubee.pos.billing.domain.Bill;
import com.kubee.pos.billing.domain.BillRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data implements the domain's {@link BillRepository}. Tenant filtering is done by Hibernate. */
interface JpaBillRepository extends JpaRepository<Bill, Long>, BillRepository {
}
