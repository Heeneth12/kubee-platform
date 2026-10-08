package com.kubee.pos.billing.application.query;

import com.kubee.pos.common.cqrs.QueryHandler;
import com.kubee.pos.common.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class SearchBillsHandler implements QueryHandler<SearchBillsQuery, PageResult<BillSummaryView>> {

    private final BillQueryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<BillSummaryView> handle(SearchBillsQuery query) {
        return repository.searchBills(query);
    }
}
