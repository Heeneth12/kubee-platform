package com.kubee.pos.shift.application.command;

import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.shift.application.port.CashSalesPort;
import com.kubee.pos.shift.domain.Shift;
import com.kubee.pos.shift.domain.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
class CloseShiftHandler implements CommandHandler<CloseShiftCommand, Void> {

    private final ShiftRepository shiftRepository;
    private final CashSalesPort cashSales;

    @Override
    @Transactional
    public Void handle(CloseShiftCommand command) {
        Shift shift = shiftRepository.findByUuid(command.shiftUuid())
                .orElseThrow(() -> NotFoundException.of("Shift", command.shiftUuid()));
        shift.close(command.countedCash(), cashSales.netCash(shift.getOpenedAt(), LocalDateTime.now()),
                command.notes(), TenantContext.currentUserUuid().orElse(null));
        shiftRepository.save(shift);
        return null;
    }
}
