package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ShiftsHandler implements QueryHandler<ShiftsQuery, ShiftReport> {

    private final ReportQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ShiftReport handle(ShiftsQuery query) {
        return repository.shifts(query.period());
    }
}
