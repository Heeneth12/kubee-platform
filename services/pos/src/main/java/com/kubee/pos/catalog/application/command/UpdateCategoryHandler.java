package com.kubee.pos.catalog.application.command;

import com.kubee.pos.catalog.domain.CatalogConstraints;
import com.kubee.pos.catalog.domain.Category;
import com.kubee.pos.catalog.domain.CategoryRepository;
import com.kubee.pos.common.application.ConflictException;
import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class UpdateCategoryHandler implements CommandHandler<UpdateCategoryCommand, String> {

    private final CategoryRepository categoryRepository;
    private final CatalogConstraints constraints;

    @Override
    @Transactional
    public String handle(UpdateCategoryCommand command) {
        Category category = categoryRepository.findByUuid(command.categoryUuid())
                .orElseThrow(() -> NotFoundException.of("Category", command.categoryUuid()));
        Category parent = command.parentUuid() == null ? null
                : categoryRepository.findByUuid(command.parentUuid())
                        .orElseThrow(() -> NotFoundException.of("Parent category", command.parentUuid()));
        boolean hasChildren = constraints.categoryHasChildren(category.getId());
        category.update(command.name(), parent, hasChildren, command.imageUrl(), command.sortOrder(), command.active());
        if (constraints.isCategoryNameTaken(category.getName(), category.getParentId(), category.getId())) {
            throw new ConflictException("Category " + category.getName() + " already exists");
        }
        return categoryRepository.save(category).getUuid();
    }
}
