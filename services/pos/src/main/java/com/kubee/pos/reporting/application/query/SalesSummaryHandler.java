package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class SalesSummaryHandler implements QueryHandler<SalesSummaryQuery, SalesSummaryReport> {

    private final ReportQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public SalesSummaryReport handle(SalesSummaryQuery query) {
        return repository.salesSummary(query.period());
    }
}
