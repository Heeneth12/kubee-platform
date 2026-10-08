package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.ordering.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class RecordPaymentHandler implements CommandHandler<RecordPaymentCommand, Void> {

    private final OrderCommandSupport support;

    @Override
    @Transactional
    public Void handle(RecordPaymentCommand command) {
        Order order = support.load(command.orderUuid());
        if (order.findPaymentByClientRef(command.payment().clientRef()).isPresent()) {
            return null; // already recorded (retry from the device)
        }
        order.recordPayment(command.payment(), support.currentUser());
        support.save(order);
        return null;
    }
}
