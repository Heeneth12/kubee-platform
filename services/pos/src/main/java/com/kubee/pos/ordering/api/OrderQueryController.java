package com.kubee.pos.ordering.api;

import com.kubee.pos.common.cqrs.QueryBus;
import com.kubee.pos.common.web.ApiResponse;
import com.kubee.pos.common.web.PageResult;
import com.kubee.pos.ordering.application.query.GetOrderQuery;
import com.kubee.pos.ordering.application.query.OrderSummaryView;
import com.kubee.pos.ordering.application.query.OrderView;
import com.kubee.pos.ordering.application.query.SearchOrdersQuery;
import com.kubee.pos.ordering.domain.OrderSource;
import com.kubee.pos.ordering.domain.OrderStatus;
import com.kubee.pos.ordering.domain.OrderType;
import com.kubee.pos.ordering.domain.PaymentStatus;
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
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderQueryController {

    private final QueryBus queryBus;

    /**
     * Held orders: {@code ?status=HELD}. Today's orders: {@code ?from=2026-10-05&to=2026-10-05}.
     * Zomato orders: {@code ?source=ZOMATO}.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<OrderSummaryView>>> search(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) OrderType orderType,
            @RequestParam(required = false) OrderSource source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + SearchOrdersQuery.DEFAULT_SIZE) int size) {
        return ApiResponse.ok(queryBus.ask(
                new SearchOrdersQuery(status, paymentStatus, orderType, source, from, to, search, page, size)));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<OrderView>> get(@PathVariable String uuid) {
        return ApiResponse.ok(queryBus.ask(new GetOrderQuery(uuid)));
    }
}
