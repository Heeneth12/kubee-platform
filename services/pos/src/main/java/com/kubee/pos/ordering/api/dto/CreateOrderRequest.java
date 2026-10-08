package com.kubee.pos.ordering.api.dto;

import com.kubee.pos.ordering.application.command.CreateOrderCommand;
import com.kubee.pos.ordering.domain.CustomerDetails;
import com.kubee.pos.ordering.domain.OrderSource;
import com.kubee.pos.ordering.domain.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Everything optional: an empty body opens an empty order. {@code clientRef} (a uuid made on the device)
 * makes the call safe to retry: the same value returns the same order. {@code source} defaults to POS;
 * Zomato / Swiggy orders carry the aggregator's order id in {@code externalOrderId}.
 */
public record CreateOrderRequest(
        @Size(max = 64) String clientRef,
        OrderType orderType,
        @Size(max = 255) String customerName,
        @Size(max = 20) String customerPhone,
        @Size(max = 50) String tableLabel,
        String notes,
        OrderSource source,
        @Size(max = 64) String externalOrderId,
        @Valid List<OrderLineRequest> lines,
        @Valid DiscountRequest discount
) {

    public CreateOrderCommand toCommand() {
        return new CreateOrderCommand(clientRef,
                new CustomerDetails(orderType, customerName, customerPhone, tableLabel, notes, source,
                        externalOrderId),
                lines == null ? List.of() : lines.stream().map(OrderLineRequest::toInput).toList(),
                discount == null ? null : discount.toDiscount(),
                discount == null ? null : discount.reason());
    }
}
