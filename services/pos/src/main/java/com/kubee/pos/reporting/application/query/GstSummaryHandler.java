package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class GstSummaryHandler implements QueryHandler<GstSummaryQuery, GstReport> {

    private final ReportQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public GstReport handle(GstSummaryQuery query) {
        return repository.gstSummary(query.period());
    }
}
