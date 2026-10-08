package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.Command;
import com.kubee.pos.ordering.domain.PaymentSpec;

/** Sending the same {@code payment.clientRef} again for the order does nothing the second time. */
public record RecordPaymentCommand(String orderUuid, PaymentSpec payment) implements Command<Void> {
}
