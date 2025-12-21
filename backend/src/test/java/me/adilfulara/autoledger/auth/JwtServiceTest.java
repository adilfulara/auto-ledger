package me.adilfulara.autoledger.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}.
 * Tests JWT parsing, validation, signature verification, and error handling.
 *
 * <p>Uses a testable subclass that injects a mock JWKSet to avoid network calls.
 */
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private TestableJwtService jwtService;
    private AuthProperties authProperties;
    private RSAKey rsaKey;
    private JWSSigner signer;

    private static final String ISSUER = "https://test-issuer.com";
    private static final String AUDIENCE = "test-audience";
    private static final String SUBJECT = "user_123";
    private static final String EMAIL = "test@example.com";

    @BeforeEach
    void setUp() throws JOSEException {
        // Generate RSA key pair for testing
        rsaKey = new RSAKeyGenerator(2048)
            .keyID("test-key-id")
            .generate();
        signer = new RSASSASigner(rsaKey);

        // Setup auth properties
        authProperties = new AuthProperties();
        authProperties.setIssuerUri(ISSUER);
        authProperties.setAudience(AUDIENCE);
        authProperties.setEnabled(true);

        // Create testable JwtService with injected JWKSet
        JWKSet jwkSet = new JWKSet(rsaKey.toPublicJWK());
        jwtService = new TestableJwtService(authProperties, jwkSet);
    }

    @Nested
    @DisplayName("Valid token validation")
    class ValidTokenValidation {

        @Test
        @DisplayName("should successfully validate and parse a valid token")
        void shouldValidateValidToken() throws Exception {
            // Given
            String token = createValidToken();

            // When
            JWTClaimsSet claims = jwtService.validateAndParse(token);

            // Then
            assertThat(claims.getSubject()).isEqualTo(SUBJECT);
            assertThat(claims.getStringClaim("email")).isEqualTo(EMAIL);
            assertThat(claims.getIssuer()).isEqualTo(ISSUER);
            assertThat(claims.getAudience()).contains(AUDIENCE);
        }
    }

    @Nested
    @DisplayName("Invalid token scenarios")
    class InvalidTokenScenarios {

        @Test
        @DisplayName("should throw exception for malformed token")
        void shouldThrowExceptionForMalformedToken() {
            // Given
            String malformedToken = "not.a.valid.jwt.token";

            // When / Then
            assertThatThrownBy(() -> jwtService.validateAndParse(malformedToken))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("Malformed JWT");
        }

        @Test
        @DisplayName("should throw exception for token with invalid issuer")
        void shouldThrowExceptionForInvalidIssuer() throws Exception {
            // Given
            String token = createTokenWithIssuer("https://wrong-issuer.com");

            // When / Then
            assertThatThrownBy(() -> jwtService.validateAndParse(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("Invalid issuer");
        }

        @Test
        @DisplayName("should throw exception for token with invalid audience")
        void shouldThrowExceptionForInvalidAudience() throws Exception {
            // Given
            String token = createTokenWithAudience("wrong-audience");

            // When / Then
            assertThatThrownBy(() -> jwtService.validateAndParse(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("Invalid audience");
        }

        @Test
        @DisplayName("should throw exception for expired token")
        void shouldThrowExceptionForExpiredToken() throws Exception {
            // Given
            String expiredToken = createExpiredToken();

            // When / Then
            assertThatThrownBy(() -> jwtService.validateAndParse(expiredToken))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("Token expired");
        }

        @Test
        @DisplayName("should throw exception for token without expiration")
        void shouldThrowExceptionForTokenWithoutExpiration() throws Exception {
            // Given
            String token = createTokenWithoutExpiration();

            // When / Then
            assertThatThrownBy(() -> jwtService.validateAndParse(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("Token expired");
        }

        @Test
        @DisplayName("should throw exception when key ID not found in JWKS")
        void shouldThrowExceptionWhenKeyIdNotFound() throws Exception {
            // Given
            RSAKey differentKey = new RSAKeyGenerator(2048)
                .keyID("different-key-id")
                .generate();
            JWSSigner differentSigner = new RSASSASigner(differentKey);

            String token = createTokenWithSigner(differentSigner, "different-key-id");

            // When / Then
            assertThatThrownBy(() -> jwtService.validateAndParse(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("No matching key found in JWKS");
        }

        @Test
        @DisplayName("should throw exception for token with invalid signature")
        void shouldThrowExceptionForInvalidSignature() throws Exception {
            // Given - create token signed with different key than in JWKS
            RSAKey wrongKey = new RSAKeyGenerator(2048)
                .keyID("test-key-id")  // Same key ID but different key
                .generate();
            JWSSigner wrongSigner = new RSASSASigner(wrongKey);

            String token = createTokenWithSigner(wrongSigner, "test-key-id");

            // When / Then
            assertThatThrownBy(() -> jwtService.validateAndParse(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("Invalid JWT signature");
        }
    }

    // ==================== Helper Methods ====================

    private String createValidToken() throws JOSEException {
        return createToken(ISSUER, AUDIENCE, new Date(System.currentTimeMillis() + 3600000));
    }

    private String createExpiredToken() throws JOSEException {
        return createToken(ISSUER, AUDIENCE, new Date(System.currentTimeMillis() - 3600000));
    }

    private String createTokenWithIssuer(String issuer) throws JOSEException {
        return createToken(issuer, AUDIENCE, new Date(System.currentTimeMillis() + 3600000));
    }

    private String createTokenWithAudience(String audience) throws JOSEException {
        return createToken(ISSUER, audience, new Date(System.currentTimeMillis() + 3600000));
    }

    private String createTokenWithoutExpiration() throws JOSEException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(SUBJECT)
            .claim("email", EMAIL)
            .issuer(ISSUER)
            .audience(AUDIENCE)
            // No expiration time
            .issueTime(new Date())
            .build();

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
            claimsSet
        );

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private String createToken(String issuer, String audience, Date expirationTime) throws JOSEException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(SUBJECT)
            .claim("email", EMAIL)
            .issuer(issuer)
            .audience(audience)
            .expirationTime(expirationTime)
            .issueTime(new Date())
            .build();

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
            claimsSet
        );

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private String createTokenWithSigner(JWSSigner customSigner, String keyId) throws JOSEException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(SUBJECT)
            .claim("email", EMAIL)
            .issuer(ISSUER)
            .audience(AUDIENCE)
            .expirationTime(new Date(System.currentTimeMillis() + 3600000))
            .issueTime(new Date())
            .build();

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(),
            claimsSet
        );

        signedJWT.sign(customSigner);
        return signedJWT.serialize();
    }

    /**
     * Testable version of JwtService that allows injecting a JWKSet for testing.
     * Overrides getJwkSet to return the test JWKSet instead of fetching from URL.
     */
    private static class TestableJwtService extends JwtService {
        private final JWKSet testJwkSet;

        public TestableJwtService(AuthProperties authProperties, JWKSet testJwkSet) {
            super(authProperties);
            this.testJwkSet = testJwkSet;
        }

        @Override
        protected JWKSet getJwkSet(String issuerUri) {
            return testJwkSet;
        }
    }
}
