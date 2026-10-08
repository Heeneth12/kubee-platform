package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.ordering.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class UpdateOrderDetailsHandler implements CommandHandler<UpdateOrderDetailsCommand, Void> {

    private final OrderCommandSupport support;

    @Override
    @Transactional
    public Void handle(UpdateOrderDetailsCommand command) {
        Order order = support.load(command.orderUuid());
        order.updateDetails(command.details());
        support.requireNotEnteredYet(order.getSource(), order.getExternalOrderId(), order.getUuid());
        support.save(order);
        return null;
    }
}
