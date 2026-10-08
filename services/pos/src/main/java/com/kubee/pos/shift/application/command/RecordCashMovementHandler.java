package com.kubee.pos.shift.application.command;

import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.shift.domain.Shift;
import com.kubee.pos.shift.domain.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class RecordCashMovementHandler implements CommandHandler<RecordCashMovementCommand, Void> {

    private final ShiftRepository shiftRepository;

    @Override
    @Transactional
    public Void handle(RecordCashMovementCommand command) {
        Shift shift = shiftRepository.findByUuid(command.shiftUuid())
                .orElseThrow(() -> NotFoundException.of("Shift", command.shiftUuid()));
        shift.recordCash(command.type(), command.amount(), command.reason(),
                TenantContext.currentUserUuid().orElse(null));
        shiftRepository.save(shift);
        return null;
    }
}
