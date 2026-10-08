package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.ordering.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ApplyOrderDiscountHandler implements CommandHandler<ApplyOrderDiscountCommand, Void> {

    private final OrderCommandSupport support;

    @Override
    @Transactional
    public Void handle(ApplyOrderDiscountCommand command) {
        Order order = support.load(command.orderUuid());
        order.applyDiscount(command.discount(), command.reason(), support.roundOff());
        support.save(order);
        return null;
    }
}
