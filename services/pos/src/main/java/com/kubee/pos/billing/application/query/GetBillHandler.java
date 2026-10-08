package com.kubee.pos.billing.application.query;

import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class GetBillHandler implements QueryHandler<GetBillQuery, BillView> {

    private final BillQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public BillView handle(GetBillQuery query) {
        return repository.findBill(query.billUuid())
                .orElseThrow(() -> NotFoundException.of("Bill", query.billUuid()));
    }
}
