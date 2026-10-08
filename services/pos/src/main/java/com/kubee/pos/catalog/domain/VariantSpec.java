package com.kubee.pos.catalog.domain;

import com.kubee.pos.common.domain.Money;

/**
 * Desired state of one variant. {@code uuid == null} adds a new variant; a known uuid updates it;
 * existing variants missing from the list are removed.
 */
public record VariantSpec(
        String uuid,
        String name,
        String itemCode,
        String barcode,
        Money sellingPrice,
        Money mrp,
        boolean isDefault,
        int sortOrder,
        boolean active
) {
}
