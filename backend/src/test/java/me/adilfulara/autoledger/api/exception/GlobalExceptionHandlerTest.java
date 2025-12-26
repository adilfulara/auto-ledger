package me.adilfulara.autoledger.api.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import me.adilfulara.autoledger.api.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(webRequest.getDescription(false)).thenReturn("uri=/api/cars");
    }

    @Nested
    @DisplayName("handleDuplicateKey and handleDataIntegrityViolation")
    class HandleDataIntegrityViolation {

        @Test
        @DisplayName("returns 409 with friendly message for duplicate VIN (DuplicateKeyException)")
        void returns409ForDuplicateVinDuplicateKeyException() {
            var cause = new PSQLException(
                    "ERROR: duplicate key value violates unique constraint \"cars_vin_key\"",
                    PSQLState.UNIQUE_VIOLATION);
            var ex = new DuplicateKeyException("Could not execute statement", cause);

            ResponseEntity<ErrorResponse> response = handler.handleDuplicateKey(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().message()).contains("VIN already exists");
        }

        @Test
        @DisplayName("returns 409 with friendly message for duplicate VIN (DataIntegrityViolationException)")
        void returns409ForDuplicateVinDataIntegrityViolationException() {
            var cause = new PSQLException(
                    "ERROR: duplicate key value violates unique constraint \"cars_vin_key\"",
                    PSQLState.UNIQUE_VIOLATION);
            var ex = new DataIntegrityViolationException("Could not execute statement", cause);

            ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().message()).contains("VIN already exists");
        }

        @Test
        @DisplayName("returns 409 with friendly message for duplicate email")
        void returns409ForDuplicateEmail() {
            var cause = new PSQLException(
                    "ERROR: duplicate key value violates unique constraint \"users_email_key\"",
                    PSQLState.UNIQUE_VIOLATION);
            var ex = new DataIntegrityViolationException("Could not execute statement", cause);

            ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().message()).contains("email already exists");
        }

        @Test
        @DisplayName("returns 409 with friendly message for duplicate auth provider ID")
        void returns409ForDuplicateAuthProviderId() {
            var cause = new PSQLException(
                    "ERROR: duplicate key value violates unique constraint \"users_auth_provider_id_key\"",
                    PSQLState.UNIQUE_VIOLATION);
            var ex = new DataIntegrityViolationException("Could not execute statement", cause);

            ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().message()).contains("auth provider ID already exists");
        }

        @Test
        @DisplayName("returns 409 with message for not-null constraint violation")
        void returns409ForNotNullViolation() {
            var cause = new PSQLException(
                    "ERROR: null value in column \"name\" violates not-null constraint",
                    PSQLState.NOT_NULL_VIOLATION);
            var ex = new DataIntegrityViolationException("Could not execute statement", cause);

            ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().message()).contains("required field is missing");
        }

        @Test
        @DisplayName("returns 409 with message for check constraint violation")
        void returns409ForCheckViolation() {
            var cause = new PSQLException(
                    "ERROR: new row for relation \"cars\" violates check constraint \"cars_year_check\"",
                    PSQLState.CHECK_VIOLATION);
            var ex = new DataIntegrityViolationException("Could not execute statement", cause);

            ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().message()).contains("out of allowed range");
        }

        @Test
        @DisplayName("returns 409 with generic message for unknown constraint")
        void returns409ForUnknownConstraint() {
            var cause = new PSQLException(
                    "ERROR: some unknown constraint violation",
                    PSQLState.UNKNOWN_STATE);
            var ex = new DataIntegrityViolationException("Could not execute statement", cause);

            ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().message()).isEqualTo("Data conflict occurred");
        }
    }

    @Nested
    @DisplayName("Logging behavior")
    class LoggingBehavior {

        private ListAppender<ILoggingEvent> listAppender;

        @BeforeEach
        void setUpLogging() {
            Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
            listAppender = new ListAppender<>();
            listAppender.start();
            logger.addAppender(listAppender);
        }

        @Test
        @DisplayName("logs ERROR with stack trace for generic exceptions")
        void logsErrorWithStackTraceForGenericException() {
            var ex = new RuntimeException("Unexpected failure");

            handler.handleGenericException(ex, webRequest);

            assertThat(listAppender.list)
                    .anyMatch(event ->
                            event.getLevel() == Level.ERROR &&
                            event.getThrowableProxy() != null &&
                            event.getFormattedMessage().contains("/api/cars"));
        }

        @Test
        @DisplayName("logs WARN without stack trace for 404")
        void logsWarnWithoutStackTraceFor404() {
            var ex = new ResourceNotFoundException("Car", UUID.randomUUID());

            handler.handleResourceNotFound(ex, webRequest);

            assertThat(listAppender.list)
                    .anyMatch(event ->
                            event.getLevel() == Level.WARN &&
                            event.getThrowableProxy() == null);
        }

        @Test
        @DisplayName("logs WARN for data integrity violations")
        void logsWarnForDataIntegrityViolations() {
            var cause = new PSQLException(
                    "ERROR: duplicate key value violates unique constraint \"cars_vin_key\"",
                    PSQLState.UNIQUE_VIOLATION);
            var ex = new DataIntegrityViolationException("Could not execute statement", cause);

            handler.handleDataIntegrityViolation(ex, webRequest);

            assertThat(listAppender.list)
                    .anyMatch(event -> event.getLevel() == Level.WARN);
        }

        @Test
        @DisplayName("logs ERROR with stack trace for IllegalStateException (non-auth)")
        void logsErrorForIllegalStateException() {
            var ex = new IllegalStateException("Some unexpected state");

            handler.handleIllegalState(ex, webRequest);

            assertThat(listAppender.list)
                    .anyMatch(event ->
                            event.getLevel() == Level.ERROR &&
                            event.getThrowableProxy() != null);
        }

        @Test
        @DisplayName("logs WARN for IllegalStateException (auth)")
        void logsWarnForAuthIllegalStateException() {
            var ex = new IllegalStateException("No authenticated user found");

            handler.handleIllegalState(ex, webRequest);

            assertThat(listAppender.list)
                    .anyMatch(event -> event.getLevel() == Level.WARN);
        }
    }
}
