package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class GetAddonGroupHandler implements QueryHandler<GetAddonGroupQuery, AddonGroupView> {

    private final CatalogQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public AddonGroupView handle(GetAddonGroupQuery query) {
        return repository.findAddonGroup(query.addonGroupUuid())
                .orElseThrow(() -> NotFoundException.of("Add-on group", query.addonGroupUuid()));
    }
}
