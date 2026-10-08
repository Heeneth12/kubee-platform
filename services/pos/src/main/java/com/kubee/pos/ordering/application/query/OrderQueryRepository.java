package com.kubee.pos.ordering.application.query;

import com.kubee.pos.common.web.PageResult;

import java.util.Optional;

/** Read model for orders. Reads straight from the tables; never loads aggregates. */
public interface OrderQueryRepository {

    Optional<OrderView> findOrder(String orderUuid);

    PageResult<OrderSummaryView> searchOrders(SearchOrdersQuery query);
}
