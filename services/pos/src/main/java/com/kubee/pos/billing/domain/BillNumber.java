package com.kubee.pos.billing.domain;

import com.kubee.pos.common.domain.Guard;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * {@code <prefix><series>/<yy-yy>/<000123>}, e.g. {@code KPA/26-27/000123}. GST rules: unique per
 * financial year, at most 16 characters, only letters, digits, '-' and '/'.
 * Each device/counter gets its own series so offline numbering never collides.
 */
public final class BillNumber {

    public static final int MAX_LENGTH = 16;
    private static final Pattern CODE = Pattern.compile("[A-Z0-9]*");

    private BillNumber() {
    }

    public static String normaliseSeries(String series) {
        String value = Guard.trimToNull(series);
        value = value == null ? "A" : value.toUpperCase(Locale.ROOT);
        Guard.isTrue(value.length() <= 4 && CODE.matcher(value).matches(), "Series must be 1 to 4 letters or digits");
        return value;
    }

    public static String format(String prefix, String series, FinancialYear year, long sequence) {
        String head = (prefix == null ? "" : prefix.trim().toUpperCase(Locale.ROOT)) + series;
        Guard.isTrue(CODE.matcher(head).matches(), "Bill prefix can only have letters and digits");
        String number = head + "/" + year.shortLabel() + "/" + String.format("%06d", sequence);
        Guard.isTrue(number.length() <= MAX_LENGTH, "Bill number " + number + " is longer than "
                + MAX_LENGTH + " characters. Use a shorter bill prefix or series.");
        return number;
    }
}
