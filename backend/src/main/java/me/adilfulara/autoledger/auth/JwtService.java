package me.adilfulara.autoledger.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for validating JWTs using JWKS (JSON Web Key Set).
 *
 * <p>Fetches public keys from the issuer's /.well-known/jwks.json endpoint
 * and caches them for performance. Validates JWT signature, expiration,
 * issuer, and audience claims.
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final AuthProperties authProperties;

    // Cache JWKS by issuer URI to avoid repeated HTTP calls
    private final ConcurrentHashMap<String, JWKSet> jwksCache = new ConcurrentHashMap<>();

    public JwtService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /**
     * Validate and parse a JWT token.
     *
     * @param token the JWT string (without "Bearer " prefix)
     * @return the validated JWT claims
     * @throws JwtValidationException if validation fails
     */
    public JWTClaimsSet validateAndParse(String token) {
        try {
            // Parse the JWT
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Get claims before validation for issuer check
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Validate issuer
            if (!authProperties.getIssuerUri().equals(claims.getIssuer())) {
                throw new JwtValidationException("Invalid issuer: " + claims.getIssuer());
            }

            // Validate audience
            if (!claims.getAudience().contains(authProperties.getAudience())) {
                throw new JwtValidationException("Invalid audience: " + claims.getAudience());
            }

            // Validate expiration
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                throw new JwtValidationException("Token expired");
            }

            // Fetch JWKS and verify signature
            JWKSet jwkSet = getJwkSet(authProperties.getIssuerUri());
            RSAKey rsaKey = (RSAKey) jwkSet.getKeyByKeyId(signedJWT.getHeader().getKeyID());

            if (rsaKey == null) {
                throw new JwtValidationException("No matching key found in JWKS");
            }

            JWSVerifier verifier = new RSASSAVerifier(rsaKey);
            if (!signedJWT.verify(verifier)) {
                throw new JwtValidationException("Invalid JWT signature");
            }

            return claims;

        } catch (ParseException e) {
            throw new JwtValidationException("Malformed JWT", e);
        } catch (JOSEException e) {
            throw new JwtValidationException("JWT verification failed", e);
        }
    }

    /**
     * Fetch JWKS from the issuer's /.well-known/jwks.json endpoint.
     * Results are cached to avoid repeated HTTP calls.
     *
     * @param issuerUri the OIDC issuer URI
     * @return the JWKSet containing public keys
     */
    protected JWKSet getJwkSet(String issuerUri) {
        return jwksCache.computeIfAbsent(issuerUri, uri -> {
            try {
                String jwksUri = uri + "/.well-known/jwks.json";
                logger.info("Fetching JWKS from: {}", jwksUri);
                return JWKSet.load(new URL(jwksUri));
            } catch (Exception e) {
                throw new JwtValidationException("Failed to fetch JWKS from " + uri, e);
            }
        });
    }

    /**
     * Clear the JWKS cache. Useful for key rotation or testing.
     */
    public void clearJwksCache() {
        jwksCache.clear();
    }
}
