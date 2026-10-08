package com.kubee.pos.billing.application.query;

import com.kubee.pos.billing.domain.TaxType;

import java.math.BigDecimal;

/** One row of the tax table at the bottom of the invoice: "CGST 2.5% on 400.00 = 10.00". */
public record TaxSummaryView(TaxType taxType, BigDecimal rate, BigDecimal taxableAmount, BigDecimal taxAmount) {
}
