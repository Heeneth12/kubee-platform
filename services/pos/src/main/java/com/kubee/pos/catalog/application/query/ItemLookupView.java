package com.kubee.pos.catalog.application.query;

/** Result of a scan / typed code. {@code matchedVariantUuid} is set when the code belongs to a variant. */
public record ItemLookupView(String matchedVariantUuid, ItemView item) {
}
