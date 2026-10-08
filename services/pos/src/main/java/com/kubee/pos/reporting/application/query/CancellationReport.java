package com.kubee.pos.reporting.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Report 5: everything undone in the period: cancelled orders, cancelled bills and refunds (by when it happened). */
public record CancellationReport(
        LocalDate from,
        LocalDate to,
        long cancelledOrderCount,
        BigDecimal cancelledOrderValue,
        long cancelledBillCount,
        BigDecimal cancelledBillValue,
        long refundCount,
        BigDecimal refundAmount,
        List<CancelledOrder> cancelledOrders,
        List<CancelledBill> cancelledBills,
        List<Refund> refunds
) {

    public record CancelledOrder(String orderUuid, String orderNumber, LocalDateTime createdAt,
                                 LocalDateTime cancelledAt, String cancelledBy, String reason, int lineCount,
                                 BigDecimal orderValue) {
    }

    public record CancelledBill(String billUuid, String billNumber, String orderNumber, LocalDateTime billDate,
                                LocalDateTime cancelledAt, String cancelledBy, String reason, BigDecimal billValue) {
    }

    public record Refund(String paymentUuid, String orderUuid, String orderNumber, String method, BigDecimal amount,
                         String reason, LocalDateTime refundedAt, String refundedBy) {
    }
}
