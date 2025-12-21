package me.adilfulara.autoledger.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AuthProperties}.
 * Tests configuration properties getters and setters.
 */
@DisplayName("AuthProperties Unit Tests")
class AuthPropertiesTest {

    private AuthProperties authProperties;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
    }

    @Test
    @DisplayName("should set and get issuer URI")
    void shouldSetAndGetIssuerUri() {
        // Given
        String issuerUri = "https://example.com";

        // When
        authProperties.setIssuerUri(issuerUri);

        // Then
        assertThat(authProperties.getIssuerUri()).isEqualTo(issuerUri);
    }

    @Test
    @DisplayName("should set and get audience")
    void shouldSetAndGetAudience() {
        // Given
        String audience = "test-audience";

        // When
        authProperties.setAudience(audience);

        // Then
        assertThat(authProperties.getAudience()).isEqualTo(audience);
    }

    @Test
    @DisplayName("enabled should default to true")
    void enabledShouldDefaultToTrue() {
        // When / Then
        assertThat(authProperties.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should set and get enabled")
    void shouldSetAndGetEnabled() {
        // Given / When
        authProperties.setEnabled(false);

        // Then
        assertThat(authProperties.isEnabled()).isFalse();
    }
}
