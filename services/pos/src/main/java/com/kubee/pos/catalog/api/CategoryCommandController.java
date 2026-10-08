package com.kubee.pos.catalog.api;

import com.kubee.pos.catalog.api.dto.CategoryRequest;
import com.kubee.pos.catalog.application.command.DeleteCategoryCommand;
import com.kubee.pos.catalog.application.query.CategoryView;
import com.kubee.pos.catalog.application.query.GetCategoryQuery;
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
@RequestMapping("/api/v1/catalog/categories")
@RequiredArgsConstructor
public class CategoryCommandController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryView>> create(@Valid @RequestBody CategoryRequest request) {
        String uuid = commandBus.send(request.toCreateCommand());
        return ApiResponse.created(queryBus.ask(new GetCategoryQuery(uuid)));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<CategoryView>> update(@PathVariable String uuid,
                                                            @Valid @RequestBody CategoryRequest request) {
        commandBus.send(request.toUpdateCommand(uuid));
        return ApiResponse.ok(queryBus.ask(new GetCategoryQuery(uuid)));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String uuid) {
        commandBus.send(new DeleteCategoryCommand(uuid));
        return ApiResponse.of(HttpStatus.OK, "Category deleted", null);
    }
}
