package com.kubee.pos.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Same envelope as the other Kubee services: {@code {code, message, data}}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return of(HttpStatus.OK, "OK", data);
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return of(HttpStatus.CREATED, "Created", data);
    }

    public static <T> ResponseEntity<ApiResponse<T>> of(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status).body(new ApiResponse<>(status.value(), message, data));
    }
}
