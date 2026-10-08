package com.kubee.pos.catalog.application.query;

import java.math.BigDecimal;

public record VariantView(
        String uuid,
        String name,
        String itemCode,
        String barcode,
        BigDecimal sellingPrice,
        BigDecimal mrp,
        boolean isDefault,
        int sortOrder,
        boolean active
) {
}
