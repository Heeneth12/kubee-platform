package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.ordering.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class HoldOrderHandler implements CommandHandler<HoldOrderCommand, Void> {

    private final OrderCommandSupport support;

    @Override
    @Transactional
    public Void handle(HoldOrderCommand command) {
        Order order = support.load(command.orderUuid());
        order.hold();
        support.save(order);
        return null;
    }
}
