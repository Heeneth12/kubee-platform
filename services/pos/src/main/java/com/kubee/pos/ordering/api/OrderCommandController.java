package com.kubee.pos.ordering.api;

import com.kubee.pos.common.cqrs.CommandBus;
import com.kubee.pos.common.cqrs.QueryBus;
import com.kubee.pos.common.web.ApiResponse;
import com.kubee.pos.ordering.api.dto.CancelOrderRequest;
import com.kubee.pos.ordering.api.dto.ChangeOrderLineRequest;
import com.kubee.pos.ordering.api.dto.CreateOrderRequest;
import com.kubee.pos.ordering.api.dto.DiscountRequest;
import com.kubee.pos.ordering.api.dto.OrderDetailsRequest;
import com.kubee.pos.ordering.api.dto.OrderLineRequest;
import com.kubee.pos.ordering.application.command.AddOrderLineCommand;
import com.kubee.pos.ordering.application.command.ApplyOrderDiscountCommand;
import com.kubee.pos.ordering.application.command.CancelOrderCommand;
import com.kubee.pos.ordering.application.command.ChangeOrderLineCommand;
import com.kubee.pos.ordering.application.command.CompleteOrderCommand;
import com.kubee.pos.ordering.application.command.HoldOrderCommand;
import com.kubee.pos.ordering.application.command.RecallOrderCommand;
import com.kubee.pos.ordering.application.command.RemoveOrderLineCommand;
import com.kubee.pos.ordering.application.command.UpdateOrderDetailsCommand;
import com.kubee.pos.ordering.application.query.GetOrderQuery;
import com.kubee.pos.ordering.application.query.OrderView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Every endpoint returns the whole order afterwards, so the billing screen can just re-render it. */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderCommandController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderView>> create(@Valid @RequestBody(required = false) CreateOrderRequest request) {
        CreateOrderRequest body = request == null
                ? new CreateOrderRequest(null, null, null, null, null, null, null, null, null, null)
                : request;
        String uuid = commandBus.send(body.toCommand());
        return ApiResponse.created(order(uuid));
    }

    @PutMapping("/{uuid}/details")
    public ResponseEntity<ApiResponse<OrderView>> updateDetails(@PathVariable String uuid,
                                                                @Valid @RequestBody OrderDetailsRequest request) {
        commandBus.send(new UpdateOrderDetailsCommand(uuid, request.toDetails()));
        return ApiResponse.ok(order(uuid));
    }

    /** Scanning the same plain item again bumps the existing line's quantity instead of adding a row. */
    @PostMapping("/{uuid}/lines")
    public ResponseEntity<ApiResponse<OrderView>> addLine(@PathVariable String uuid,
                                                          @Valid @RequestBody OrderLineRequest request) {
        commandBus.send(new AddOrderLineCommand(uuid, request.toInput()));
        return ApiResponse.ok(order(uuid));
    }

    @PatchMapping("/{uuid}/lines/{lineUuid}")
    public ResponseEntity<ApiResponse<OrderView>> changeLine(@PathVariable String uuid, @PathVariable String lineUuid,
                                                             @Valid @RequestBody ChangeOrderLineRequest request) {
        commandBus.send(new ChangeOrderLineCommand(uuid, lineUuid, request.quantity(), request.discount(),
                request.notes()));
        return ApiResponse.ok(order(uuid));
    }

    @DeleteMapping("/{uuid}/lines/{lineUuid}")
    public ResponseEntity<ApiResponse<OrderView>> removeLine(@PathVariable String uuid, @PathVariable String lineUuid) {
        commandBus.send(new RemoveOrderLineCommand(uuid, lineUuid));
        return ApiResponse.ok(order(uuid));
    }

    @PutMapping("/{uuid}/discount")
    public ResponseEntity<ApiResponse<OrderView>> applyDiscount(@PathVariable String uuid,
                                                                @Valid @RequestBody DiscountRequest request) {
        commandBus.send(new ApplyOrderDiscountCommand(uuid, request.toDiscount(), request.reason()));
        return ApiResponse.ok(order(uuid));
    }

    @DeleteMapping("/{uuid}/discount")
    public ResponseEntity<ApiResponse<OrderView>> removeDiscount(@PathVariable String uuid) {
        commandBus.send(new ApplyOrderDiscountCommand(uuid, null, null));
        return ApiResponse.ok(order(uuid));
    }

    @PostMapping("/{uuid}/hold")
    public ResponseEntity<ApiResponse<OrderView>> hold(@PathVariable String uuid) {
        commandBus.send(new HoldOrderCommand(uuid));
        return ApiResponse.ok(order(uuid));
    }

    @PostMapping("/{uuid}/recall")
    public ResponseEntity<ApiResponse<OrderView>> recall(@PathVariable String uuid) {
        commandBus.send(new RecallOrderCommand(uuid));
        return ApiResponse.ok(order(uuid));
    }

    @PostMapping("/{uuid}/complete")
    public ResponseEntity<ApiResponse<OrderView>> complete(@PathVariable String uuid) {
        commandBus.send(new CompleteOrderCommand(uuid));
        return ApiResponse.ok(order(uuid));
    }

    @PostMapping("/{uuid}/cancel")
    public ResponseEntity<ApiResponse<OrderView>> cancel(@PathVariable String uuid,
                                                         @Valid @RequestBody CancelOrderRequest request) {
        commandBus.send(new CancelOrderCommand(uuid, request.reason()));
        return ApiResponse.ok(order(uuid));
    }

    private OrderView order(String uuid) {
        return queryBus.ask(new GetOrderQuery(uuid));
    }
}
