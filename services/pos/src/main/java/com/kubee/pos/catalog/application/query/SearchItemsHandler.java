package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import com.kubee.pos.common.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class SearchItemsHandler implements QueryHandler<SearchItemsQuery, PageResult<ItemView>> {

    private final CatalogQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ItemView> handle(SearchItemsQuery query) {
        return repository.searchItems(query);
    }
}
