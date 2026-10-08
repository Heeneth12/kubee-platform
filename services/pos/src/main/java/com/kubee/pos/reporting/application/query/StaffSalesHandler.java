package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class StaffSalesHandler implements QueryHandler<StaffSalesQuery, StaffSalesReport> {

    private final ReportQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public StaffSalesReport handle(StaffSalesQuery query) {
        return repository.staffSales(query.period());
    }
}
