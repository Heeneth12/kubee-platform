package com.kubee.pos.ordering.api;

import com.kubee.pos.common.cqrs.CommandBus;
import com.kubee.pos.common.cqrs.QueryBus;
import com.kubee.pos.common.domain.Money;
import com.kubee.pos.common.web.ApiResponse;
import com.kubee.pos.ordering.api.dto.PaymentRequest;
import com.kubee.pos.ordering.api.dto.RefundRequest;
import com.kubee.pos.ordering.application.command.RecordPaymentCommand;
import com.kubee.pos.ordering.application.command.RefundPaymentCommand;
import com.kubee.pos.ordering.application.query.GetOrderQuery;
import com.kubee.pos.ordering.application.query.OrderView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Payments hang off the order. Each call returns the whole order (status, due, change). */
@RestController
@RequestMapping("/api/v1/orders/{orderUuid}/payments")
@RequiredArgsConstructor
public class PaymentCommandController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    /** Split payment = one call per method. The order completes itself when nothing is left to pay. */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderView>> pay(@PathVariable String orderUuid,
                                                      @Valid @RequestBody PaymentRequest request) {
        commandBus.send(new RecordPaymentCommand(orderUuid, request.toSpec()));
        return ApiResponse.ok(queryBus.ask(new GetOrderQuery(orderUuid)));
    }

    @PostMapping("/{paymentUuid}/refund")
    public ResponseEntity<ApiResponse<OrderView>> refund(@PathVariable String orderUuid,
                                                         @PathVariable String paymentUuid,
                                                         @Valid @RequestBody RefundRequest request) {
        commandBus.send(new RefundPaymentCommand(orderUuid, paymentUuid, Money.of(request.amount()),
                request.method(), request.reason(), request.clientRef()));
        return ApiResponse.ok(queryBus.ask(new GetOrderQuery(orderUuid)));
    }
}
