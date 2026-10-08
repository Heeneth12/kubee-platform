package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.Command;
import com.kubee.pos.ordering.domain.Discount;

import java.math.BigDecimal;

/** Quantity, line discount (null = none) and note. To change item, variant or add-ons, remove and re-add the line. */
public record ChangeOrderLineCommand(String orderUuid, String lineUuid, BigDecimal quantity, Discount discount,
                                     String notes) implements Command<Void> {
}
