package com.kubee.pos.ordering.domain;

import com.kubee.pos.common.domain.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shop setting (pos_settings.round_off_mode): how the bill total is rounded for the customer. */
public enum RoundOffMode {
    NONE,
    /** 123.49 -> 123, 123.50 -> 124 */
    NEAREST_1,
    /** 123.20 -> 123.00, 123.30 -> 123.50 */
    NEAREST_0_50,
    /** 123.99 -> 123 */
    DOWN_1;

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    /** Amount to add to {@code total} (may be negative) so the customer pays a round figure. */
    public Money adjustmentFor(Money total) {
        BigDecimal amount = total.amount();
        BigDecimal rounded = switch (this) {
            case NONE -> amount;
            case NEAREST_1 -> amount.setScale(0, RoundingMode.HALF_UP);
            case NEAREST_0_50 -> amount.multiply(TWO).setScale(0, RoundingMode.HALF_UP).divide(TWO);
            case DOWN_1 -> amount.setScale(0, RoundingMode.FLOOR);
        };
        return Money.of(rounded.subtract(amount));
    }
}
