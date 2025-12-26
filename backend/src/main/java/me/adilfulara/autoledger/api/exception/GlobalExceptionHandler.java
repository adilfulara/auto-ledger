package me.adilfulara.autoledger.api.exception;

import me.adilfulara.autoledger.api.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

/**
 * Global exception handler for REST API.
 * Converts exceptions to consistent ErrorResponse format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        logger.warn("{} - Resource not found: {}", getRequestContext(request), ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidOdometerException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOdometer(
            InvalidOdometerException ex, WebRequest request) {
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ErrorResponse.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue()
                ))
                .toList();

        ErrorResponse error = ErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed for one or more fields",
                getPath(request),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DbActionExecutionException.class)
    public ResponseEntity<ErrorResponse> handleDbActionExecution(
            DbActionExecutionException ex, WebRequest request) {
        // Spring Data JDBC wraps database exceptions - unwrap to find the root cause
        Throwable cause = ex.getCause();

        if (cause instanceof DuplicateKeyException duplicateKeyEx) {
            return handleDuplicateKey(duplicateKeyEx, request);
        }

        if (cause instanceof DataIntegrityViolationException dataIntegrityEx) {
            return handleDataIntegrityViolation(dataIntegrityEx, request);
        }

        // If not a recognized database exception, treat as generic error
        logger.error("{} - Database action execution failed. Exception type: {}",
                getRequestContext(request), ex.getClass().getName(), ex);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected database error occurred",
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(
            DuplicateKeyException ex, WebRequest request) {
        String message = parseConstraintViolation(ex);
        logger.warn("{} - Duplicate key violation: {}", getRequestContext(request), message);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                message,
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        String message = parseConstraintViolation(ex);
        logger.warn("{} - Data integrity violation: {}", getRequestContext(request), message);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                message,
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, WebRequest request) {
        // IllegalStateException from CurrentUserResolver means authentication failed
        if (ex.getMessage() != null && ex.getMessage().contains("authenticated user")) {
            logger.warn("{} - Authentication required: {}", getRequestContext(request), ex.getMessage());

            ErrorResponse error = ErrorResponse.of(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    "Authentication required",
                    getPath(request)
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // Other IllegalStateExceptions are server errors
        logger.error("{} - Unexpected illegal state", getRequestContext(request), ex);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage(),
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        logger.error("{} - Unexpected error occurred", getRequestContext(request), ex);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String getRequestContext(WebRequest request) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String method = attrs != null ? attrs.getRequest().getMethod() : "UNKNOWN";
        return String.format("[%s %s]", method, getPath(request));
    }

    private String parseConstraintViolation(DataAccessException ex) {
        String rootMessage = ex.getMostSpecificCause().getMessage();

        if (rootMessage.contains("cars_vin_key")) {
            return "A car with this VIN already exists";
        }
        if (rootMessage.contains("users_email_key")) {
            return "A user with this email already exists";
        }
        if (rootMessage.contains("users_auth_provider_id_key")) {
            return "A user with this auth provider ID already exists";
        }
        if (rootMessage.contains("violates not-null constraint")) {
            return "A required field is missing";
        }
        if (rootMessage.contains("violates check constraint")) {
            return "A field value is out of allowed range";
        }

        return "Data conflict occurred";
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
