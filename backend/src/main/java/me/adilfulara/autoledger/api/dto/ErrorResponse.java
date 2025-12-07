package me.adilfulara.autoledger.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response format for API errors.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    /**
     * Represents a validation error on a specific field.
     */
    public record FieldError(
            String field,
            String message,
            Object rejectedValue
    ) {}

    /**
     * Factory method for simple error responses without field errors.
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    /**
     * Factory method for validation error responses with field errors.
     */
    public static ErrorResponse ofValidation(int status, String message, String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, "Validation Failed", message, path, fieldErrors);
    }
}
