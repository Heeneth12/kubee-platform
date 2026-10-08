package com.kubee.pos.ordering.application.query;

import com.kubee.pos.common.cqrs.Query;
import com.kubee.pos.common.web.PageResult;
import com.kubee.pos.ordering.domain.OrderSource;
import com.kubee.pos.ordering.domain.OrderStatus;
import com.kubee.pos.ordering.domain.OrderType;
import com.kubee.pos.ordering.domain.PaymentStatus;

import java.time.LocalDate;

/**
 * All filters optional; newest first. {@code from} / {@code to} are inclusive order dates.
 * {@code search} matches the order number or online order id (exact), customer phone (contains) or name (contains).
 * {@code page} is 0-based.
 */
public record SearchOrdersQuery(
        OrderStatus status,
        PaymentStatus paymentStatus,
        OrderType orderType,
        OrderSource source,
        LocalDate from,
        LocalDate to,
        String search,
        int page,
        int size
) implements Query<PageResult<OrderSummaryView>> {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    public SearchOrdersQuery {
        page = Math.max(page, 0);
        size = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }
}
