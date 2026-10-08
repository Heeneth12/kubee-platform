package com.kubee.pos.catalog.api;

import com.kubee.pos.catalog.application.query.AddonGroupView;
import com.kubee.pos.catalog.application.query.GetAddonGroupQuery;
import com.kubee.pos.catalog.application.query.ListAddonGroupsQuery;
import com.kubee.pos.common.cqrs.QueryBus;
import com.kubee.pos.common.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/addon-groups")
@RequiredArgsConstructor
public class AddonGroupQueryController {

    private final QueryBus queryBus;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddonGroupView>>> list(@RequestParam(required = false) Boolean active) {
        return ApiResponse.ok(queryBus.ask(new ListAddonGroupsQuery(active)));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AddonGroupView>> get(@PathVariable String uuid) {
        return ApiResponse.ok(queryBus.ask(new GetAddonGroupQuery(uuid)));
    }
}
