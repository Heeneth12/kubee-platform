package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.ordering.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CompleteOrderHandler implements CommandHandler<CompleteOrderCommand, Void> {

    private final OrderCommandSupport support;

    @Override
    @Transactional
    public Void handle(CompleteOrderCommand command) {
        Order order = support.load(command.orderUuid());
        order.complete();
        support.save(order);
        return null;
    }
}
