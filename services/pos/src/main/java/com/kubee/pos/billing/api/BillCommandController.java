package com.kubee.pos.billing.api;

import com.kubee.pos.billing.api.dto.CancelBillRequest;
import com.kubee.pos.billing.api.dto.IssueBillRequest;
import com.kubee.pos.billing.api.dto.ShareBillRequest;
import com.kubee.pos.billing.application.command.CancelBillCommand;
import com.kubee.pos.billing.application.command.ShareBillCommand;
import com.kubee.pos.billing.application.query.BillView;
import com.kubee.pos.billing.application.query.GetBillQuery;
import com.kubee.pos.billing.domain.ShareChannel;
import com.kubee.pos.common.cqrs.CommandBus;
import com.kubee.pos.common.cqrs.QueryBus;
import com.kubee.pos.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
public class BillCommandController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    /** Issue the GST invoice of a completed order. Calling it again returns the order's existing bill. */
    @PostMapping
    public ResponseEntity<ApiResponse<BillView>> issue(@Valid @RequestBody IssueBillRequest request) {
        String uuid = commandBus.send(request.toCommand());
        return ApiResponse.created(queryBus.ask(new GetBillQuery(uuid)));
    }

    /** An issued bill never changes: cancel it, then issue a new one for the order. */
    @PostMapping("/{uuid}/cancel")
    public ResponseEntity<ApiResponse<BillView>> cancel(@PathVariable String uuid,
                                                        @Valid @RequestBody CancelBillRequest request) {
        commandBus.send(new CancelBillCommand(uuid, request.reason()));
        return ApiResponse.ok(queryBus.ask(new GetBillQuery(uuid)));
    }

    /** Call when the bill is printed or re-printed; counts prints. */
    @PostMapping("/{uuid}/print")
    public ResponseEntity<ApiResponse<BillView>> print(@PathVariable String uuid) {
        commandBus.send(new ShareBillCommand(uuid, ShareChannel.PRINT));
        return ApiResponse.ok(queryBus.ask(new GetBillQuery(uuid)));
    }

    /** Records that the bill was sent (WhatsApp / SMS). Sending itself is done by the app for now. */
    @PostMapping("/{uuid}/share")
    public ResponseEntity<ApiResponse<BillView>> share(@PathVariable String uuid,
                                                       @Valid @RequestBody ShareBillRequest request) {
        commandBus.send(new ShareBillCommand(uuid, request.channel()));
        return ApiResponse.ok(queryBus.ask(new GetBillQuery(uuid)));
    }
}
