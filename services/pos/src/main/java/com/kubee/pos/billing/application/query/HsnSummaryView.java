package com.kubee.pos.billing.application.query;

import java.math.BigDecimal;

/** HSN/SAC-wise summary (needed on invoices and for GSTR-1). {@code hsnSacCode} null = items without a code. */
public record HsnSummaryView(String hsnSacCode, BigDecimal taxRate, BigDecimal quantity, BigDecimal taxableAmount,
                             BigDecimal taxAmount, BigDecimal totalAmount) {
}
