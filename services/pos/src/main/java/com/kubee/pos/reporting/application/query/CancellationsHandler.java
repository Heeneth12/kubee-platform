package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CancellationsHandler implements QueryHandler<CancellationsQuery, CancellationReport> {

    private final ReportQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public CancellationReport handle(CancellationsQuery query) {
        return repository.cancellations(query.period());
    }
}
