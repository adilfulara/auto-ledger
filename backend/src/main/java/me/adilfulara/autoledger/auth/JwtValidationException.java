package me.adilfulara.autoledger.auth;

/**
 * Exception thrown when JWT validation fails.
 *
 * <p>This can happen due to:
 * <ul>
 *   <li>Invalid signature</li>
 *   <li>Expired token</li>
 *   <li>Invalid issuer or audience</li>
 *   <li>Malformed JWT</li>
 * </ul>
 */
public class JwtValidationException extends RuntimeException {

    public JwtValidationException(String message) {
        super(message);
    }

    public JwtValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
