package com.kubee.pos.shift.application.query;

import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class GetShiftHandler implements QueryHandler<GetShiftQuery, ShiftView> {

    private final ShiftQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ShiftView handle(GetShiftQuery query) {
        return repository.findShift(query.shiftUuid())
                .orElseThrow(() -> NotFoundException.of("Shift", query.shiftUuid()));
    }
}
