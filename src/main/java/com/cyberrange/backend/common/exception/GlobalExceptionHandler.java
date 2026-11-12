
package com.cyberrange.backend.common.exception;

import com.cyberrange.backend.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles application exceptions globally.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles resource not found exceptions.
     *
     * @param exception the exception
     * @return HTTP 404 response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException exception
    ) {
        ApiResponse<Void> response = ApiResponse.error(
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Handles business rule violations.
     *
     * @param exception the exception
     * @return HTTP 409 response
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception
    ) {
        ApiResponse<Void> response = ApiResponse.error(
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    /**
     * Handles unexpected exceptions.
     *
     * @param exception the exception
     * @return HTTP 500 response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception
    ) {
        ApiResponse<Void> response = ApiResponse.error(
                "An unexpected error occurred"
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
