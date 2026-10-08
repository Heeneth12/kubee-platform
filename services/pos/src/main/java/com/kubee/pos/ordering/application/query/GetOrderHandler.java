package com.kubee.pos.ordering.application.query;

import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class GetOrderHandler implements QueryHandler<GetOrderQuery, OrderView> {

    private final OrderQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public OrderView handle(GetOrderQuery query) {
        return repository.findOrder(query.orderUuid())
                .orElseThrow(() -> NotFoundException.of("Order", query.orderUuid()));
    }
}
