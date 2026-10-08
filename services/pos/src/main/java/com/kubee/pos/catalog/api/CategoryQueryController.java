package com.kubee.pos.catalog.api;

import com.kubee.pos.catalog.application.query.CategoryView;
import com.kubee.pos.catalog.application.query.GetCategoryQuery;
import com.kubee.pos.catalog.application.query.ListCategoriesQuery;
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
@RequestMapping("/api/v1/catalog/categories")
@RequiredArgsConstructor
public class CategoryQueryController {

    private final QueryBus queryBus;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryView>>> list(@RequestParam(required = false) Boolean active) {
        return ApiResponse.ok(queryBus.ask(new ListCategoriesQuery(active)));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<CategoryView>> get(@PathVariable String uuid) {
        return ApiResponse.ok(queryBus.ask(new GetCategoryQuery(uuid)));
    }
}
