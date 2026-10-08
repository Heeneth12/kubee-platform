package com.kubee.pos.common.web;

import java.util.List;

/** One page of a list query. {@code page} is 0-based. */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) ((totalElements + size - 1) / size);
        return new PageResult<>(content, page, size, totalElements, totalPages);
    }
}
