package com.kubee.pos.catalog.domain;

/**
 * Rules that span more than one aggregate (uniqueness across all items, "still in use" checks).
 * Checked by command handlers before saving; the database unique indexes are the final safety net.
 */
public interface CatalogConstraints {

    /** True if another item, or a variant of another item, already uses this code. */
    boolean isItemCodeTaken(String itemCode, Long excludeItemId);

    /** True if another item, or a variant of another item, already uses this barcode. */
    boolean isBarcodeTaken(String barcode, Long excludeItemId);

    boolean isCategoryNameTaken(String name, Long parentId, Long excludeCategoryId);

    boolean isAddonGroupNameTaken(String name, Long excludeAddonGroupId);

    boolean categoryHasItems(Long categoryId);

    boolean categoryHasChildren(Long categoryId);

    boolean isAddonGroupInUse(Long addonGroupId);
}
