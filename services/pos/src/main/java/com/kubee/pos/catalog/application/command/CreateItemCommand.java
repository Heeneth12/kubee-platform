package com.kubee.pos.catalog.application.command;

import com.kubee.pos.common.cqrs.Command;

public record CreateItemCommand(ItemInput input) implements Command<String> {
}
