package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class GetItemHandler implements QueryHandler<GetItemQuery, ItemView> {

    private final CatalogQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ItemView handle(GetItemQuery query) {
        return repository.findItem(query.itemUuid())
                .orElseThrow(() -> NotFoundException.of("Item", query.itemUuid()));
    }
}
