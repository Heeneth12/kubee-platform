package com.kubee.pos.billing.application.query;

import com.kubee.pos.common.web.PageResult;

import java.util.Optional;

/** Read model for bills. Reads straight from the tables; never loads aggregates. */
public interface BillQueryRepository {

    Optional<BillView> findBill(String billUuid);

    /** The order's current ISSUED bill. */
    Optional<BillView> findIssuedBillOfOrder(String orderUuid);

    PageResult<BillSummaryView> searchBills(SearchBillsQuery query);
}
