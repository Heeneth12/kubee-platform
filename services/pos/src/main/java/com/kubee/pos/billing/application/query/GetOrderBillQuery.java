package com.kubee.pos.billing.application.query;

import com.kubee.pos.common.cqrs.Query;

/** The current (ISSUED) bill of an order. */
public record GetOrderBillQuery(String orderUuid) implements Query<BillView> {
}
