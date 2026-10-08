package com.kubee.pos.ordering.domain;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findByUuid(String uuid);

    /** Idempotency: the order a device already sent with this key. */
    Optional<Order> findByClientRef(String clientRef);

    /** Orders already entered for this Zomato / Swiggy order id (cancelled ones included). */
    List<Order> findBySourceAndExternalOrderId(OrderSource source, String externalOrderId);
}
