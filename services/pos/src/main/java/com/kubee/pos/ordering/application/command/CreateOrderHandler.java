package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.ordering.application.port.OrderNumberPort;
import com.kubee.pos.ordering.domain.LineSpec;
import com.kubee.pos.ordering.domain.Order;
import com.kubee.pos.ordering.domain.OrderRepository;
import com.kubee.pos.ordering.domain.RoundOffMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class CreateOrderHandler implements CommandHandler<CreateOrderCommand, String> {

    private final OrderRepository orderRepository;
    private final OrderCommandSupport support;
    private final OrderLineFactory lineFactory;
    private final OrderNumberPort orderNumbers;

    @Override
    @Transactional
    public String handle(CreateOrderCommand command) {
        if (command.clientRef() != null && !command.clientRef().isBlank()) {
            Optional<Order> existing = orderRepository.findByClientRef(command.clientRef().trim());
            if (existing.isPresent()) {
                return existing.get().getUuid();
            }
        }

        // Validate every line against the catalog before using up an order number.
        if (command.details() != null) {
            support.requireNotEnteredYet(command.details().source(), command.details().externalOrderId(), null);
        }
        List<LineSpec> lines = command.lines().stream().map(lineFactory::build).toList();
        RoundOffMode roundOff = support.roundOff();

        Order order = Order.open(orderNumbers.nextOrderNumber(), command.clientRef(), command.details(),
                support.currentUser());
        lines.forEach(line -> order.addLine(line, roundOff));
        if (command.discount() != null) {
            order.applyDiscount(command.discount(), command.discountReason(), roundOff);
        }
        return orderRepository.save(order).getUuid();
    }
}
