package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.ordering.domain.LineSpec;
import com.kubee.pos.ordering.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class AddOrderLineHandler implements CommandHandler<AddOrderLineCommand, Void> {

    private final OrderCommandSupport support;
    private final OrderLineFactory lineFactory;

    @Override
    @Transactional
    public Void handle(AddOrderLineCommand command) {
        Order order = support.load(command.orderUuid());
        LineSpec spec = lineFactory.build(command.line());
        order.addLine(spec, support.roundOff());
        support.save(order);
        return null;
    }
}
