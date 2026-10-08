package com.kubee.pos.billing.application.command;

import com.kubee.pos.billing.domain.Bill;
import com.kubee.pos.billing.domain.BillRepository;
import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CancelBillHandler implements CommandHandler<CancelBillCommand, Void> {

    private final BillRepository billRepository;

    @Override
    @Transactional
    public Void handle(CancelBillCommand command) {
        Bill bill = billRepository.findByUuid(command.billUuid())
                .orElseThrow(() -> NotFoundException.of("Bill", command.billUuid()));
        bill.cancel(command.reason(), TenantContext.currentUserUuid().orElse(null));
        billRepository.save(bill);
        return null;
    }
}
