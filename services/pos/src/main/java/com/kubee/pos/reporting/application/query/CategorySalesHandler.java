package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CategorySalesHandler implements QueryHandler<CategorySalesQuery, CategorySalesReport> {

    private final ReportQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public CategorySalesReport handle(CategorySalesQuery query) {
        return repository.categorySales(query.period());
    }
}
