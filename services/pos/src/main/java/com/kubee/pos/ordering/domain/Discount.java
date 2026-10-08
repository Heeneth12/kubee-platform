package com.kubee.pos.ordering.domain;

import com.kubee.pos.common.domain.Guard;
import com.kubee.pos.common.domain.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A line or order discount as the cashier entered it: 10 PERCENT, or 20 FLAT (rupees). */
public record Discount(DiscountType type, BigDecimal value) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public Discount {
        Guard.isTrue(type != null, "Discount type is required");
        Guard.isTrue(value != null && value.signum() > 0, "Discount value must be more than 0");
        value = value.setScale(2, RoundingMode.HALF_UP);
        Guard.isTrue(type != DiscountType.PERCENT || value.compareTo(HUNDRED) <= 0,
                "Discount percent cannot be more than 100");
    }

    /** Null-safe factory: no type or no value means no discount. */
    public static Discount ofNullable(DiscountType type, BigDecimal value) {
        return type == null || value == null || value.signum() == 0 ? null : new Discount(type, value);
    }

    /** Rupees off {@code base}; never more than {@code base}. */
    public Money amountOf(Money base) {
        Money off = type == DiscountType.PERCENT
                ? Money.of(base.amount().multiply(value).divide(HUNDRED, 2, RoundingMode.HALF_UP))
                : Money.of(value);
        return off.isGreaterThan(base) ? base : off;
    }

    /** A FLAT discount bigger than what it applies to is a typing mistake, not something to silently cap. */
    void checkFits(Money base, String what) {
        Guard.isTrue(type != DiscountType.FLAT || !Money.of(value).isGreaterThan(base),
                "Discount cannot be more than the " + what + " (" + base + ")");
    }
}
