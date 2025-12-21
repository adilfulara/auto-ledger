package me.adilfulara.autoledger.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtValidationException}.
 * Tests exception constructors and message/cause handling.
 */
@DisplayName("JwtValidationException Unit Tests")
class JwtValidationExceptionTest {

    @Test
    @DisplayName("constructor with message should store message")
    void constructorWithMessageShouldStoreMessage() {
        // Given
        String message = "Invalid JWT token";

        // When
        JwtValidationException exception = new JwtValidationException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("constructor with message and cause should store both")
    void constructorWithMessageAndCauseShouldStoreBoth() {
        // Given
        String message = "JWT verification failed";
        Throwable cause = new RuntimeException("Network error");

        // When
        JwtValidationException exception = new JwtValidationException(message, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("Network error");
    }
}
