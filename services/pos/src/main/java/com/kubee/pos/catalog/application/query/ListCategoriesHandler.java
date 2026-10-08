package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
class ListCategoriesHandler implements QueryHandler<ListCategoriesQuery, List<CategoryView>> {

    private final CatalogQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryView> handle(ListCategoriesQuery query) {
        return repository.findCategories(query.active());
    }
}
