package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ChannelSalesHandler implements QueryHandler<ChannelSalesQuery, ChannelSalesReport> {

    private final ReportQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ChannelSalesReport handle(ChannelSalesQuery query) {
        return repository.channelSales(query.period());
    }
}
