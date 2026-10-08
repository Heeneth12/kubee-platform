package com.kubee.pos.ordering.application.query;

import com.kubee.pos.ordering.domain.OrderSource;
import com.kubee.pos.ordering.domain.OrderStatus;
import com.kubee.pos.ordering.domain.OrderType;
import com.kubee.pos.ordering.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of the order list (held orders, today's orders, search). */
public record OrderSummaryView(
        String uuid,
        String orderNumber,
        OrderType orderType,
        OrderSource source,
        String externalOrderId,
        OrderStatus status,
        PaymentStatus paymentStatus,
        String customerName,
        String customerPhone,
        String tableLabel,
        int lineCount,
        BigDecimal grandTotal,
        BigDecimal paidAmount,
        BigDecimal dueAmount,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
