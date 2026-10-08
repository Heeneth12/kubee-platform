package com.kubee.pos.catalog.application.command;

import com.kubee.pos.common.cqrs.Command;

public record DeleteItemCommand(String itemUuid) implements Command<Void> {
}
