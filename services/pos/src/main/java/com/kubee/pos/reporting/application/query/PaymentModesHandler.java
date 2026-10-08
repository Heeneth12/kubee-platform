package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class PaymentModesHandler implements QueryHandler<PaymentModesQuery, PaymentModeReport> {

    private final ReportQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public PaymentModeReport handle(PaymentModesQuery query) {
        return repository.paymentModes(query.period());
    }
}
