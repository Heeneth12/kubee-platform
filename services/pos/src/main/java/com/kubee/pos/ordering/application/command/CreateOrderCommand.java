package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.Command;
import com.kubee.pos.ordering.domain.CustomerDetails;
import com.kubee.pos.ordering.domain.Discount;

import java.util.List;

/**
 * Open a new order, optionally with its lines and bill discount in one go (instant billing / offline sync).
 * Sending the same {@code clientRef} again returns the order created the first time.
 */
public record CreateOrderCommand(
        String clientRef,
        CustomerDetails details,
        List<LineInput> lines,
        Discount discount,
        String discountReason
) implements Command<String> {

    public CreateOrderCommand {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
