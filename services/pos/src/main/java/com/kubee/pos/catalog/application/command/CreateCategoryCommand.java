package com.kubee.pos.catalog.application.command;

import com.kubee.pos.common.cqrs.Command;

public record CreateCategoryCommand(String name, String parentUuid, String imageUrl, int sortOrder) implements Command<String> {
}
