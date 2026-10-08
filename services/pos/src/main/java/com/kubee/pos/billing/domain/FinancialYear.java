package com.kubee.pos.billing.domain;

import com.kubee.pos.common.domain.Guard;

import java.time.LocalDate;

/** Indian financial year, April to March by default: 5 Oct 2026 is in "2026-27". */
public record FinancialYear(int startYear) {

    public static FinancialYear of(LocalDate date, int startMonth) {
        Guard.isTrue(startMonth >= 1 && startMonth <= 12, "Financial year start month must be 1 to 12");
        return new FinancialYear(date.getMonthValue() >= startMonth ? date.getYear() : date.getYear() - 1);
    }

    /** "2026-27": the bill counter restarts for each of these. */
    public String key() {
        return startYear + "-" + twoDigits(startYear + 1);
    }

    /** "26-27": as printed inside the bill number. */
    public String shortLabel() {
        return twoDigits(startYear) + "-" + twoDigits(startYear + 1);
    }

    private static String twoDigits(int year) {
        return String.format("%02d", year % 100);
    }
}
