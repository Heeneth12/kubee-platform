package com.kubee.pos.catalog.api;

import com.kubee.pos.catalog.api.dto.AddonGroupRequest;
import com.kubee.pos.catalog.application.command.DeleteAddonGroupCommand;
import com.kubee.pos.catalog.application.query.AddonGroupView;
import com.kubee.pos.catalog.application.query.GetAddonGroupQuery;
import com.kubee.pos.common.cqrs.CommandBus;
import com.kubee.pos.common.cqrs.QueryBus;
import com.kubee.pos.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/addon-groups")
@RequiredArgsConstructor
public class AddonGroupCommandController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @PostMapping
    public ResponseEntity<ApiResponse<AddonGroupView>> create(@Valid @RequestBody AddonGroupRequest request) {
        String uuid = commandBus.send(request.toCreateCommand());
        return ApiResponse.created(queryBus.ask(new GetAddonGroupQuery(uuid)));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AddonGroupView>> update(@PathVariable String uuid,
                                                              @Valid @RequestBody AddonGroupRequest request) {
        commandBus.send(request.toUpdateCommand(uuid));
        return ApiResponse.ok(queryBus.ask(new GetAddonGroupQuery(uuid)));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String uuid) {
        commandBus.send(new DeleteAddonGroupCommand(uuid));
        return ApiResponse.of(HttpStatus.OK, "Add-on group deleted", null);
    }
}
