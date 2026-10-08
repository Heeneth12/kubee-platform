package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.Command;
import com.kubee.pos.ordering.domain.CustomerDetails;

/** Full replace of customer name/phone, order type, table, notes and the online order id. */
public record UpdateOrderDetailsCommand(String orderUuid, CustomerDetails details) implements Command<Void> {
}
