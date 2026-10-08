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
class CreateCategoryHandler implements CommandHandler<CreateCategoryCommand, String> {

    private final CategoryRepository categoryRepository;
    private final CatalogConstraints constraints;

    @Override
    @Transactional
    public String handle(CreateCategoryCommand command) {
        Category parent = command.parentUuid() == null ? null
                : categoryRepository.findByUuid(command.parentUuid())
                        .orElseThrow(() -> NotFoundException.of("Parent category", command.parentUuid()));
        Category category = Category.create(command.name(), parent, command.imageUrl(), command.sortOrder());
        if (constraints.isCategoryNameTaken(category.getName(), category.getParentId(), null)) {
            throw new ConflictException("Category " + category.getName() + " already exists");
        }
        return categoryRepository.save(category).getUuid();
    }
}
