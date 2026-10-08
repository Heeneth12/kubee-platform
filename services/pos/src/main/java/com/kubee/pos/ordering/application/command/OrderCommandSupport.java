package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.application.ConflictException;
import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.domain.Guard;
import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.ordering.application.port.OrderSettingsPort;
import com.kubee.pos.ordering.domain.Order;
import com.kubee.pos.ordering.domain.OrderRepository;
import com.kubee.pos.ordering.domain.OrderSource;
import com.kubee.pos.ordering.domain.OrderStatus;
import com.kubee.pos.ordering.domain.RoundOffMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Shared by the order command handlers: load the order, read the round-off rule, who is acting. */
@Component
@RequiredArgsConstructor
class OrderCommandSupport {

    private final OrderRepository orderRepository;
    private final OrderSettingsPort settings;

    Order load(String orderUuid) {
        return orderRepository.findByUuid(orderUuid)
                .orElseThrow(() -> NotFoundException.of("Order", orderUuid));
    }

    Order save(Order order) {
        return orderRepository.save(order);
    }

    /**
     * Stops the same Zomato / Swiggy order being entered twice. A cancelled entry doesn't count, so a
     * wrongly entered order can be cancelled and entered again.
     */
    void requireNotEnteredYet(OrderSource source, String externalOrderId, String exceptOrderUuid) {
        String id = Guard.trimToNull(externalOrderId);
        if (source == null || !source.isOnline() || id == null) {
            return;
        }
        orderRepository.findBySourceAndExternalOrderId(source, id).stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED && !o.getUuid().equals(exceptOrderUuid))
                .findFirst()
                .ifPresent(o -> {
                    throw new ConflictException(source + " order " + id + " is already entered as order #"
                            + o.getOrderNumber());
                });
    }

    RoundOffMode roundOff() {
        return settings.roundOffMode();
    }

    String currentUser() {
        return TenantContext.currentUserUuid().orElse(null);
    }
}
