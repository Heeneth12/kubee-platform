package com.kubee.pos.ordering.application.query;

import com.kubee.pos.ordering.domain.DiscountType;
import com.kubee.pos.ordering.domain.OrderSource;
import com.kubee.pos.ordering.domain.OrderStatus;
import com.kubee.pos.ordering.domain.OrderType;
import com.kubee.pos.ordering.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** One order with its lines and payments: everything the billing screen shows. */
public record OrderView(
        String uuid,
        String orderNumber,
        String clientRef,
        OrderType orderType,
        OrderSource source,
        String externalOrderId,
        OrderStatus status,
        PaymentStatus paymentStatus,
        String customerName,
        String customerPhone,
        String tableLabel,
        String notes,
        BigDecimal subTotal,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        String discountReason,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        BigDecimal roundOffAmount,
        BigDecimal grandTotal,
        BigDecimal paidAmount,
        BigDecimal dueAmount,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        String cancelledBy,
        String cancelReason,
        List<OrderLineView> lines,
        List<PaymentView> payments
) {
}
