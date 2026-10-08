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
class DeleteCategoryHandler implements CommandHandler<DeleteCategoryCommand, Void> {

    private final CategoryRepository categoryRepository;
    private final CatalogConstraints constraints;

    @Override
    @Transactional
    public Void handle(DeleteCategoryCommand command) {
        Category category = categoryRepository.findByUuid(command.categoryUuid())
                .orElseThrow(() -> NotFoundException.of("Category", command.categoryUuid()));
        if (constraints.categoryHasChildren(category.getId())) {
            throw new ConflictException("Category has sub-categories; delete or move them first");
        }
        if (constraints.categoryHasItems(category.getId())) {
            throw new ConflictException("Category has items; move or delete them first");
        }
        category.delete();
        categoryRepository.save(category);
        return null;
    }
}
