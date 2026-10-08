package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.domain.Guard;
import com.kubee.pos.common.time.ShopTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Inclusive date range of a report, in shop (India) dates. Both ends default to today. */
public record ReportPeriod(LocalDate from, LocalDate to) {

    public static final int MAX_DAYS = 366;

    public ReportPeriod {
        LocalDate today = ShopTime.today();
        from = from == null ? (to == null ? today : to) : from;
        to = to == null ? (from.isAfter(today) ? from : today) : to;
        Guard.isTrue(!from.isAfter(to), "'from' must be on or before 'to'");
        Guard.isTrue(ChronoUnit.DAYS.between(from, to) < MAX_DAYS, "A report can cover at most " + MAX_DAYS + " days");
    }

    public LocalDateTime start() {
        return from.atStartOfDay();
    }

    /** Exclusive end: midnight after {@code to}. */
    public LocalDateTime end() {
        return to.plusDays(1).atStartOfDay();
    }
}
