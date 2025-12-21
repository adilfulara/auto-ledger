package me.adilfulara.autoledger.auth;

import java.security.Principal;
import java.util.UUID;

/**
 * Represents an authenticated user in the system.
 * Extracted from JWT claims and used throughout request processing.
 *
 * <p>Uses dual-identity pattern:
 * <ul>
 *   <li>{@code userId} - Internal stable UUID (database primary key)</li>
 *   <li>{@code authProviderId} - External auth provider ID (JWT 'sub' claim)</li>
 * </ul>
 */
public record AuthenticatedUser(
    UUID userId,
    String authProviderId,
    String email
) implements Principal {

    @Override
    public String getName() {
        return authProviderId;
    }
}
