package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class HourlySalesHandler implements QueryHandler<HourlySalesQuery, HourlySalesReport> {

    private final ReportQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public HourlySalesReport handle(HourlySalesQuery query) {
        return repository.hourlySales(query.period());
    }
}
