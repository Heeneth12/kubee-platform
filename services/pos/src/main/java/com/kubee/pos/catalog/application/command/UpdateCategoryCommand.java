package com.kubee.pos.catalog.application.command;

import com.kubee.pos.common.cqrs.Command;

public record UpdateCategoryCommand(String categoryUuid, String name, String parentUuid, String imageUrl, int sortOrder, boolean active) implements Command<String> {
}
