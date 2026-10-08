package com.kubee.pos.billing.domain;

import com.kubee.pos.common.domain.Money;

/** Order totals, copied onto the bill when it is issued. */
public record BillTotals(
        Money subTotal,
        Money discountAmount,
        Money taxableAmount,
        Money taxAmount,
        Money roundOffAmount,
        Money grandTotal
) {
}
