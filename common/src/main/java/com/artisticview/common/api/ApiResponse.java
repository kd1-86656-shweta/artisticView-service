package com.artisticview.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Uniform response envelope: every endpoint returns {@code { data, error, meta }}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, ApiError error, Map<String, Object> meta) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, Map<String, Object> meta) {
        return new ApiResponse<>(data, null, meta);
    }

    public static <T> ApiResponse<T> fail(ApiError error) {
        return new ApiResponse<>(null, error, null);
    }
}
