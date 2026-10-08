package com.kubee.pos.billing.domain;

import java.math.BigDecimal;

/** One part of a tax group, e.g. CGST 2.5 of "GST 5%". {@code id} may be null for a derived split. */
public record TaxComponentSpec(Long id, TaxType type, String name, BigDecimal rate) {
}
