package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.ordering.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ChangeOrderLineHandler implements CommandHandler<ChangeOrderLineCommand, Void> {

    private final OrderCommandSupport support;

    @Override
    @Transactional
    public Void handle(ChangeOrderLineCommand command) {
        Order order = support.load(command.orderUuid());
        order.changeLine(command.lineUuid(), command.quantity(), command.discount(), command.notes(),
                support.roundOff());
        support.save(order);
        return null;
    }
}
