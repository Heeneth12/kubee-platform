package com.kubee.pos.shift.application.command;

import com.kubee.pos.common.application.ConflictException;
import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.shift.domain.Shift;
import com.kubee.pos.shift.domain.ShiftRepository;
import com.kubee.pos.shift.domain.ShiftStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class OpenShiftHandler implements CommandHandler<OpenShiftCommand, String> {

    private final ShiftRepository shiftRepository;

    @Override
    @Transactional
    public String handle(OpenShiftCommand command) {
        shiftRepository.findFirstByStatus(ShiftStatus.OPEN).ifPresent(open -> {
            throw new ConflictException("A shift is already open (since " + open.getOpenedAt().withNano(0)
                    + "). Close it first.");
        });
        Shift shift = Shift.open(command.openingCash(), command.notes(), TenantContext.currentUserUuid().orElse(null));
        return shiftRepository.save(shift).getUuid();
    }
}
