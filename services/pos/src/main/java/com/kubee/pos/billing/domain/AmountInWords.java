package com.kubee.pos.billing.domain;

import com.kubee.pos.common.domain.Money;

import java.math.BigDecimal;

/** "Rupees One Lakh Twenty Three Thousand Four Hundred Five and Fifty Paise Only" (Indian numbering). */
public final class AmountInWords {

    private static final String[] ONES = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight",
            "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
            "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy",
            "Eighty", "Ninety"};

    private AmountInWords() {
    }

    public static String of(Money money) {
        BigDecimal amount = money.amount().abs();
        long rupees = amount.longValue();
        int paise = amount.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        StringBuilder words = new StringBuilder("Rupees ").append(rupees == 0 ? "Zero" : indian(rupees));
        if (paise > 0) {
            words.append(" and ").append(belowHundred(paise)).append(" Paise");
        }
        return words.append(" Only").toString();
    }

    private static String indian(long n) {
        StringBuilder out = new StringBuilder();
        n = part(out, n, 10_000_000L, "Crore");
        n = part(out, n, 100_000L, "Lakh");
        n = part(out, n, 1_000L, "Thousand");
        n = part(out, n, 100L, "Hundred");
        if (n > 0) {
            append(out, belowHundred((int) n));
        }
        return out.toString();
    }

    private static long part(StringBuilder out, long n, long unit, String name) {
        long count = n / unit;
        if (count > 0) {
            append(out, (unit == 10_000_000L ? indian(count) : belowHundred((int) count)) + " " + name);
        }
        return n % unit;
    }

    private static String belowHundred(int n) {
        return n < 20 ? ONES[n] : (TENS[n / 10] + (n % 10 == 0 ? "" : " " + ONES[n % 10]));
    }

    private static void append(StringBuilder out, String words) {
        if (!out.isEmpty()) out.append(' ');
        out.append(words);
    }
}
