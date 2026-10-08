package com.kubee.pos.shift.application.query;

import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class GetCurrentShiftHandler implements QueryHandler<GetCurrentShiftQuery, ShiftView> {

    private final ShiftQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ShiftView handle(GetCurrentShiftQuery query) {
        return repository.findOpenShift().orElseThrow(() -> new NotFoundException("No shift is open"));
    }
}
