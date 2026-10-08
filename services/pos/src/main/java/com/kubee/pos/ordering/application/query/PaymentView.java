package com.kubee.pos.ordering.application.query;

import com.kubee.pos.ordering.domain.PaymentMethod;
import com.kubee.pos.ordering.domain.PaymentType;
import com.kubee.pos.ordering.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** {@code refundOfPaymentUuid} is set on REFUND rows; {@code refundableAmount} on PAYMENT rows. */
public record PaymentView(
        String uuid,
        String clientRef,
        PaymentType type,
        PaymentMethod method,
        TransactionStatus status,
        BigDecimal amount,
        BigDecimal tenderedAmount,
        BigDecimal changeAmount,
        BigDecimal refundableAmount,
        String referenceNo,
        String refundOfPaymentUuid,
        String notes,
        LocalDateTime paidAt,
        String receivedBy
) {
}
