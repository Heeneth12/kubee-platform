package com.kubee.pos.billing.application.command;

import com.kubee.pos.billing.domain.Bill;
import com.kubee.pos.billing.domain.BillRepository;
import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ShareBillHandler implements CommandHandler<ShareBillCommand, Void> {

    private final BillRepository billRepository;

    @Override
    @Transactional
    public Void handle(ShareBillCommand command) {
        Bill bill = billRepository.findByUuid(command.billUuid())
                .orElseThrow(() -> NotFoundException.of("Bill", command.billUuid()));
        bill.recordShare(command.channel());
        billRepository.save(bill);
        return null;
    }
}
