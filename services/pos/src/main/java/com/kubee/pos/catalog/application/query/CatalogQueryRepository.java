package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.web.PageResult;

import java.util.List;
import java.util.Optional;

/** Read model for the catalog. Reads straight from the tables; never loads aggregates. */
public interface CatalogQueryRepository {

    List<CategoryView> findCategories(Boolean active);

    Optional<CategoryView> findCategory(String categoryUuid);

    PageResult<ItemView> searchItems(SearchItemsQuery query);

    Optional<ItemView> findItem(String itemUuid);

    /** Active item or active variant whose code or barcode equals {@code code}. */
    Optional<ItemLookupView> lookupByCode(String code);

    List<AddonGroupView> findAddonGroups(Boolean active);

    Optional<AddonGroupView> findAddonGroup(String addonGroupUuid);
}
