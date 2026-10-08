package com.kubee.pos.catalog.application.query;

import com.kubee.pos.catalog.domain.FoodType;
import com.kubee.pos.common.cqrs.Query;
import com.kubee.pos.common.web.PageResult;

/**
 * All filters optional. {@code search} matches name (contains), or item/variant code or barcode (exact).
 * {@code page} is 0-based.
 */
public record SearchItemsQuery(
        String search,
        String categoryUuid,
        Boolean active,
        Boolean favourite,
        FoodType foodType,
        int page,
        int size
) implements Query<PageResult<ItemView>> {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 500;

    public SearchItemsQuery {
        page = Math.max(page, 0);
        size = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }
}
