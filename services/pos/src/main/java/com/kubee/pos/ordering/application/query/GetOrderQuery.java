package com.kubee.pos.ordering.application.query;

import com.kubee.pos.common.cqrs.Query;

public record GetOrderQuery(String orderUuid) implements Query<OrderView> {
}
