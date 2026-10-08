package com.kubee.pos.billing.api;

import com.kubee.pos.billing.application.query.BillSummaryView;
import com.kubee.pos.billing.application.query.BillView;
import com.kubee.pos.billing.application.query.GetBillQuery;
import com.kubee.pos.billing.application.query.GetOrderBillQuery;
import com.kubee.pos.billing.application.query.SearchBillsQuery;
import com.kubee.pos.billing.domain.BillStatus;
import com.kubee.pos.common.cqrs.QueryBus;
import com.kubee.pos.common.web.ApiResponse;
import com.kubee.pos.common.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BillQueryController {

    private final QueryBus queryBus;

    @GetMapping("/bills")
    public ResponseEntity<ApiResponse<PageResult<BillSummaryView>>> search(
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + SearchBillsQuery.DEFAULT_SIZE) int size) {
        return ApiResponse.ok(queryBus.ask(new SearchBillsQuery(status, from, to, search, page, size)));
    }

    @GetMapping("/bills/{uuid}")
    public ResponseEntity<ApiResponse<BillView>> get(@PathVariable String uuid) {
        return ApiResponse.ok(queryBus.ask(new GetBillQuery(uuid)));
    }

    /** The order's current (issued) bill; 404 if it has none. */
    @GetMapping("/orders/{orderUuid}/bill")
    public ResponseEntity<ApiResponse<BillView>> ofOrder(@PathVariable String orderUuid) {
        return ApiResponse.ok(queryBus.ask(new GetOrderBillQuery(orderUuid)));
    }
}
