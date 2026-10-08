package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.Command;

/** Only needed when nothing is due (e.g. a 0 total); orders complete themselves on the last payment. */
public record CompleteOrderCommand(String orderUuid) implements Command<Void> {
}
