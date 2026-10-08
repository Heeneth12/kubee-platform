package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import com.kubee.pos.common.domain.Guard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
class Gstr1Handler implements QueryHandler<Gstr1Query, Map<String, Object>> {

    private final ReportQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> handle(Gstr1Query query) {
        Guard.isTrue(query.month() != null, "Month is required (yyyy-MM)");
        Gstr1Data data = repository.gstr1(query.month());
        Guard.isTrue(data.sellerGstin() != null, "Add the shop's GSTIN in settings before creating GSTR-1");
        return Gstr1Builder.build(data);
    }
}
