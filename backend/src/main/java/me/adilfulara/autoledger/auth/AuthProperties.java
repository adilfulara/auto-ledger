package me.adilfulara.autoledger.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for JWT authentication.
 *
 * <p>Maps to {@code auth.jwt.*} properties in application.yml.
 *
 * <p>Example configuration:
 * <pre>
 * auth:
 *   jwt:
 *     enabled: true
 *     issuer-uri: https://clerk.your-domain.com
 *     audience: auto-ledger
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class AuthProperties {

    /**
     * Whether JWT authentication is enabled.
     * Set to false in local dev to bypass auth.
     */
    private boolean enabled = true;

    /**
     * The OIDC issuer URI (e.g., https://clerk.your-domain.com).
     * Used to fetch JWKS (JSON Web Key Set) from /.well-known/jwks.json
     */
    private String issuerUri;

    /**
     * Expected audience claim (aud) in the JWT.
     * Typically the application identifier.
     */
    private String audience;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }
}
