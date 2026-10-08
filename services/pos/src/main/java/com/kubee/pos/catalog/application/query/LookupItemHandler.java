package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class LookupItemHandler implements QueryHandler<LookupItemQuery, ItemLookupView> {

    private final CatalogQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ItemLookupView handle(LookupItemQuery query) {
        return repository.lookupByCode(query.code())
                .orElseThrow(() -> new NotFoundException("No active item with code or barcode " + query.code()));
    }
}
