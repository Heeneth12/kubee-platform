package com.kubee.pos.shift.api;

import com.kubee.pos.common.cqrs.CommandBus;
import com.kubee.pos.common.cqrs.QueryBus;
import com.kubee.pos.common.domain.Money;
import com.kubee.pos.common.web.ApiResponse;
import com.kubee.pos.shift.api.dto.CashMovementRequest;
import com.kubee.pos.shift.api.dto.CloseShiftRequest;
import com.kubee.pos.shift.api.dto.OpenShiftRequest;
import com.kubee.pos.shift.application.command.CloseShiftCommand;
import com.kubee.pos.shift.application.command.OpenShiftCommand;
import com.kubee.pos.shift.application.command.RecordCashMovementCommand;
import com.kubee.pos.shift.application.query.GetCurrentShiftQuery;
import com.kubee.pos.shift.application.query.GetShiftQuery;
import com.kubee.pos.shift.application.query.ShiftView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cash drawer: open, cash in/out, close with count. Shift history is in /api/v1/reports/shifts (managers). */
@RestController
@RequestMapping("/api/v1/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @PostMapping
    public ResponseEntity<ApiResponse<ShiftView>> open(@Valid @RequestBody OpenShiftRequest request) {
        String uuid = commandBus.send(new OpenShiftCommand(Money.of(request.openingCash()), request.notes()));
        return ApiResponse.created(queryBus.ask(new GetShiftQuery(uuid)));
    }

    /** The open shift with live expected cash; 404 when none is open. */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<ShiftView>> current() {
        return ApiResponse.ok(queryBus.ask(new GetCurrentShiftQuery()));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ShiftView>> get(@PathVariable String uuid) {
        return ApiResponse.ok(queryBus.ask(new GetShiftQuery(uuid)));
    }

    @PostMapping("/{uuid}/cash-movements")
    public ResponseEntity<ApiResponse<ShiftView>> cashMovement(@PathVariable String uuid,
                                                               @Valid @RequestBody CashMovementRequest request) {
        commandBus.send(new RecordCashMovementCommand(uuid, request.type(), Money.of(request.amount()),
                request.reason()));
        return ApiResponse.ok(queryBus.ask(new GetShiftQuery(uuid)));
    }

    @PostMapping("/{uuid}/close")
    public ResponseEntity<ApiResponse<ShiftView>> close(@PathVariable String uuid,
                                                        @Valid @RequestBody CloseShiftRequest request) {
        commandBus.send(new CloseShiftCommand(uuid, Money.of(request.countedCash()), request.notes()));
        return ApiResponse.ok(queryBus.ask(new GetShiftQuery(uuid)));
    }
}
