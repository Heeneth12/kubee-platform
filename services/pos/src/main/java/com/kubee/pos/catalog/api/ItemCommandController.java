package com.kubee.pos.catalog.api;

import com.kubee.pos.catalog.api.dto.ActiveRequest;
import com.kubee.pos.catalog.api.dto.FavouriteRequest;
import com.kubee.pos.catalog.api.dto.ItemRequest;
import com.kubee.pos.catalog.application.command.ChangeItemActiveCommand;
import com.kubee.pos.catalog.application.command.ChangeItemFavouriteCommand;
import com.kubee.pos.catalog.application.command.CreateItemCommand;
import com.kubee.pos.catalog.application.command.DeleteItemCommand;
import com.kubee.pos.catalog.application.command.UpdateItemCommand;
import com.kubee.pos.catalog.application.query.GetItemQuery;
import com.kubee.pos.catalog.application.query.ItemView;
import com.kubee.pos.common.cqrs.CommandBus;
import com.kubee.pos.common.cqrs.QueryBus;
import com.kubee.pos.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/items")
@RequiredArgsConstructor
public class ItemCommandController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @PostMapping
    public ResponseEntity<ApiResponse<ItemView>> create(@Valid @RequestBody ItemRequest request) {
        String uuid = commandBus.send(new CreateItemCommand(request.toInput()));
        return ApiResponse.created(queryBus.ask(new GetItemQuery(uuid)));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ItemView>> update(@PathVariable String uuid,
                                                        @Valid @RequestBody ItemRequest request) {
        commandBus.send(new UpdateItemCommand(uuid, request.toInput()));
        return ApiResponse.ok(queryBus.ask(new GetItemQuery(uuid)));
    }

    /** Quick "available / not available today" toggle from the billing screen. */
    @PatchMapping("/{uuid}/active")
    public ResponseEntity<ApiResponse<ItemView>> changeActive(@PathVariable String uuid,
                                                              @Valid @RequestBody ActiveRequest request) {
        commandBus.send(new ChangeItemActiveCommand(uuid, request.active()));
        return ApiResponse.ok(queryBus.ask(new GetItemQuery(uuid)));
    }

    @PatchMapping("/{uuid}/favourite")
    public ResponseEntity<ApiResponse<ItemView>> changeFavourite(@PathVariable String uuid,
                                                                 @Valid @RequestBody FavouriteRequest request) {
        commandBus.send(new ChangeItemFavouriteCommand(uuid, request.favourite()));
        return ApiResponse.ok(queryBus.ask(new GetItemQuery(uuid)));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String uuid) {
        commandBus.send(new DeleteItemCommand(uuid));
        return ApiResponse.of(HttpStatus.OK, "Item deleted", null);
    }
}
