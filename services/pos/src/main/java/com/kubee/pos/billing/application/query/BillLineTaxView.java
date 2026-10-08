package com.kubee.pos.billing.application.query;

import com.kubee.pos.billing.domain.TaxType;

import java.math.BigDecimal;

public record BillLineTaxView(TaxType taxType, String taxName, BigDecimal rate, BigDecimal taxableAmount,
                              BigDecimal taxAmount) {
}
