package com.kubee.pos.billing.application.query;

import com.kubee.pos.billing.domain.BillStatus;
import com.kubee.pos.common.cqrs.Query;
import com.kubee.pos.common.web.PageResult;

import java.time.LocalDate;

/**
 * All filters optional; newest first. {@code from}/{@code to} are inclusive bill dates.
 * {@code search} matches bill number (contains), order number (exact), customer phone or GSTIN (contains).
 */
public record SearchBillsQuery(
        BillStatus status,
        LocalDate from,
        LocalDate to,
        String search,
        int page,
        int size
) implements Query<PageResult<BillSummaryView>> {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    public SearchBillsQuery {
        page = Math.max(page, 0);
        size = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }
}
