package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ItemSalesHandler implements QueryHandler<ItemSalesQuery, ItemSalesReport> {

    private final ReportQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ItemSalesReport handle(ItemSalesQuery query) {
        return repository.itemSales(query);
    }
}
