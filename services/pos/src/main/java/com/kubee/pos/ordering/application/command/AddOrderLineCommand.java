package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.Command;

public record AddOrderLineCommand(String orderUuid, LineInput line) implements Command<Void> {
}
