package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.common.domain.DomainException;
import com.kubee.pos.ordering.application.port.IssuedBillPort;
import com.kubee.pos.ordering.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CancelOrderHandler implements CommandHandler<CancelOrderCommand, Void> {

    private final OrderCommandSupport support;
    private final IssuedBillPort issuedBills;

    @Override
    @Transactional
    public Void handle(CancelOrderCommand command) {
        Order order = support.load(command.orderUuid());
        issuedBills.issuedBillNumber(order.getId()).ifPresent(number -> {
            throw new DomainException("Cancel bill " + number + " before cancelling this order");
        });
        order.cancel(command.reason(), support.currentUser());
        support.save(order);
        return null;
    }
}
