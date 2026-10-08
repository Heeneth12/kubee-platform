package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.ordering.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class RefundPaymentHandler implements CommandHandler<RefundPaymentCommand, Void> {

    private final OrderCommandSupport support;

    @Override
    @Transactional
    public Void handle(RefundPaymentCommand command) {
        Order order = support.load(command.orderUuid());
        if (order.findPaymentByClientRef(command.clientRef()).isPresent()) {
            return null; // already refunded (retry from the device)
        }
        order.refund(command.paymentUuid(), command.amount(), command.method(), command.reason(),
                command.clientRef(), support.currentUser());
        support.save(order);
        return null;
    }
}
