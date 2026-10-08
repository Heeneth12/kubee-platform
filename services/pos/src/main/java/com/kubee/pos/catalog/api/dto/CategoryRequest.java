package com.kubee.pos.catalog.api.dto;

import com.kubee.pos.catalog.application.command.CreateCategoryCommand;
import com.kubee.pos.catalog.application.command.UpdateCategoryCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 255) String name,
        String parentUuid,
        @Size(max = 500) String imageUrl,
        Integer sortOrder,
        Boolean active
) {

    public CreateCategoryCommand toCreateCommand() {
        return new CreateCategoryCommand(name, parentUuid, imageUrl, orZero(sortOrder));
    }

    public UpdateCategoryCommand toUpdateCommand(String categoryUuid) {
        return new UpdateCategoryCommand(categoryUuid, name, parentUuid, imageUrl, orZero(sortOrder),
                active == null || active);
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
