package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
class ListAddonGroupsHandler implements QueryHandler<ListAddonGroupsQuery, List<AddonGroupView>> {

    private final CatalogQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<AddonGroupView> handle(ListAddonGroupsQuery query) {
        return repository.findAddonGroups(query.active());
    }
}
