package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.Query;

/** @param categoryUuid optional: only items currently in this category */
public record ItemSalesQuery(ReportPeriod period, String categoryUuid, Sort sort) implements Query<ItemSalesReport> {

    public enum Sort {
        /** Highest net amount first (default). */
        AMOUNT,
        /** Most units first. */
        QUANTITY
    }

    public ItemSalesQuery {
        sort = sort == null ? Sort.AMOUNT : sort;
    }
}
