package com.kubee.pos.billing.application.query;

import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class GetOrderBillHandler implements QueryHandler<GetOrderBillQuery, BillView> {

    private final BillQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public BillView handle(GetOrderBillQuery query) {
        return repository.findIssuedBillOfOrder(query.orderUuid())
                .orElseThrow(() -> new NotFoundException("No bill issued for order " + query.orderUuid()));
    }
}
