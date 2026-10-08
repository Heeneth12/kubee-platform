package com.kubee.pos.catalog.application.command;

import com.kubee.pos.common.cqrs.Command;

public record DeleteCategoryCommand(String categoryUuid) implements Command<Void> {
}
