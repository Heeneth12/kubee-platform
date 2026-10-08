package com.kubee.pos.ordering.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import com.kubee.pos.common.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class SearchOrdersHandler implements QueryHandler<SearchOrdersQuery, PageResult<OrderSummaryView>> {

    private final OrderQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<OrderSummaryView> handle(SearchOrdersQuery query) {
        return repository.searchOrders(query);
    }
}
