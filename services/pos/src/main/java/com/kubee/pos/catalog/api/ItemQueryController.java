package com.kubee.pos.catalog.api;

import com.kubee.pos.catalog.application.query.GetItemQuery;
import com.kubee.pos.catalog.application.query.ItemLookupView;
import com.kubee.pos.catalog.application.query.ItemView;
import com.kubee.pos.catalog.application.query.LookupItemQuery;
import com.kubee.pos.catalog.application.query.SearchItemsQuery;
import com.kubee.pos.catalog.domain.FoodType;
import com.kubee.pos.common.cqrs.QueryBus;
import com.kubee.pos.common.web.ApiResponse;
import com.kubee.pos.common.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/items")
@RequiredArgsConstructor
public class ItemQueryController {

    private final QueryBus queryBus;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<ItemView>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryUuid,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean favourite,
            @RequestParam(required = false) FoodType foodType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + SearchItemsQuery.DEFAULT_SIZE) int size) {
        return ApiResponse.ok(queryBus.ask(
                new SearchItemsQuery(search, categoryUuid, active, favourite, foodType, page, size)));
    }

    /** Barcode scan or typed short code on the billing screen. */
    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<ItemLookupView>> lookup(@RequestParam String code) {
        return ApiResponse.ok(queryBus.ask(new LookupItemQuery(code)));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ItemView>> get(@PathVariable String uuid) {
        return ApiResponse.ok(queryBus.ask(new GetItemQuery(uuid)));
    }
}
