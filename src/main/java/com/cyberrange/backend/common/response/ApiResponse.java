package com.cyberrange.backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard API response wrapper.
 *
 * @param <T> Type of response data.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {

    /**
     * Creates a successful API response with data.
     *
     * @param message response message
     * @param data response data
     * @param <T> type of response data
     * @return successful API response
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                true,
                message,
                data,
                Instant.now()
        );
    }

    /**
     * Creates a successful API response without data.
     *
     * @param message response message
     * @return successful API response
     */
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(
                true,
                message,
                null,
                Instant.now()
        );
    }

    /**
     * Creates a failed API response.
     *
     * @param message error message
     * @return failed API response
     */
    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(
                false,
                message,
                null,
                Instant.now()
        );
    }
}
