package com.kubee.inventory.sales.order.repository;


import com.kubee.inventory.sales.order.entity.SalesOrder;
import com.kubee.inventory.sales.order.entity.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {

    List<SalesOrderItem> findBySalesOrderId(Long id);
}
