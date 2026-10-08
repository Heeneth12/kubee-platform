package com.kubee.pos.ordering.domain;

import com.kubee.pos.common.domain.Money;

import java.math.BigDecimal;

/**
 * Snapshot of a chosen add-on taken from the catalog when the line is added.
 *
 * @param quantity per unit of the parent line (2 = "double cheese")
 */
public record LineAddonSpec(Long addonId, String name, BigDecimal quantity, Money unitPrice) {
}
