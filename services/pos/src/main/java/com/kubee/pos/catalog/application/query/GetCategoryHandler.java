package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class GetCategoryHandler implements QueryHandler<GetCategoryQuery, CategoryView> {

    private final CatalogQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public CategoryView handle(GetCategoryQuery query) {
        return repository.findCategory(query.categoryUuid())
                .orElseThrow(() -> NotFoundException.of("Category", query.categoryUuid()));
    }
}
