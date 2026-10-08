package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.Command;
import com.kubee.pos.ordering.domain.Discount;

/** Bill-level discount; {@code discount} null removes it. */
public record ApplyOrderDiscountCommand(String orderUuid, Discount discount, String reason) implements Command<Void> {
}
