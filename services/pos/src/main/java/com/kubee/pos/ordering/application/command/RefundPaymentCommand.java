package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.Command;
import com.kubee.pos.common.domain.Money;
import com.kubee.pos.ordering.domain.PaymentMethod;

/** @param method null = same method as the original payment */
public record RefundPaymentCommand(String orderUuid, String paymentUuid, Money amount, PaymentMethod method,
                                   String reason, String clientRef) implements Command<Void> {
}
