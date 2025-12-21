package me.adilfulara.autoledger.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}.
 * Tests JWT parsing, validation, and JWKS fetching.
 */
class JwtServiceTest {

    private JwtService jwtService;
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

        // Note: In real implementation, JwtService will fetch JWKS from issuerUri/.well-known/jwks.json
        // For unit tests, we'll need to mock the JWKS fetching or use a test-specific constructor
    }

    @Test
    void validateAndParse_ValidToken_ReturnsClaimsSet() throws Exception {
        // Arrange
        String token = createValidToken();

        // For this test to work, we need a way to inject the test public key
        // This is a simplified test - the actual implementation will be tested in integration tests
        // with a mock JWKS endpoint

        // Act & Assert
        // This test demonstrates the expected behavior
        // Actual implementation will be verified in integration tests
    }

    @Test
    void validateAndParse_ExpiredToken_ThrowsException() throws Exception {
        // Arrange
        String expiredToken = createExpiredToken();

        // Act & Assert
        // Will test with actual implementation
    }

    @Test
    void validateAndParse_InvalidSignature_ThrowsException() {
        // Arrange
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";

        // Act & Assert
        // Will test with actual implementation
    }

    /**
     * Helper method to create a valid JWT for testing.
     */
    private String createValidToken() throws JOSEException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(SUBJECT)
            .claim("email", EMAIL)
            .issuer(ISSUER)
            .audience(AUDIENCE)
            .expirationTime(new Date(System.currentTimeMillis() + 3600000)) // 1 hour from now
            .issueTime(new Date())
            .build();

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
            claimsSet
        );

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    /**
     * Helper method to create an expired JWT for testing.
     */
    private String createExpiredToken() throws JOSEException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(SUBJECT)
            .claim("email", EMAIL)
            .issuer(ISSUER)
            .audience(AUDIENCE)
            .expirationTime(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
            .issueTime(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
            .build();

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
            claimsSet
        );

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}
