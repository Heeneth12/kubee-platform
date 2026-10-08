package com.kubee.pos.ordering.infrastructure.persistence;

import com.kubee.pos.ordering.domain.Order;
import com.kubee.pos.ordering.domain.OrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data implements the domain's {@link OrderRepository}. Tenant filtering is done by Hibernate. */
interface JpaOrderRepository extends JpaRepository<Order, Long>, OrderRepository {
}
