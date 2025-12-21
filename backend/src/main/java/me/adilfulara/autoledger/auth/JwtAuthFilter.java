package me.adilfulara.autoledger.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.adilfulara.autoledger.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that validates JWTs and provisions users just-in-time.
 *
 * <p>For each incoming request:
 * <ol>
 *   <li>Extract JWT from Authorization header</li>
 *   <li>Validate JWT signature and claims</li>
 *   <li>Find or create user (JIT provisioning)</li>
 *   <li>Store {@link AuthenticatedUser} in request attribute</li>
 * </ol>
 *
 * <p>If authentication is disabled (local dev), the filter passes through.
 * If JWT is missing or invalid, the filter returns 401 Unauthorized.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHENTICATED_USER_ATTRIBUTE = "authenticatedUser";

    private final AuthProperties authProperties;
    private final JwtService jwtService;
    private final JitUserService jitUserService;

    public JwtAuthFilter(AuthProperties authProperties, JwtService jwtService, JitUserService jitUserService) {
        this.authProperties = authProperties;
        this.jwtService = jwtService;
        this.jitUserService = jitUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip authentication if disabled (local dev / staging without auth provider)
        if (!authProperties.isEnabled()) {
            // Inject a test user for development/testing
            // Use the Alice user from sample data (ID from V1.1__add_sample_data.sql)
            UUID testUserId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
            AuthenticatedUser testUser = new AuthenticatedUser(
                testUserId,
                "test_user_dev",
                "dev@test.com"
            );
            request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, testUser);
            logger.debug("Auth disabled - using test user: {}", testUserId);
            filterChain.doFilter(request, response);
            return;
        }

        // Skip authentication for health check endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator/health") || path.startsWith("/actuator/info")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token from Authorization header
        String token = extractToken(request);
        if (token == null) {
            logger.debug("No JWT found in request to {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Missing or invalid Authorization header\"}");
            return;
        }

        try {
            // Validate JWT and extract claims
            JWTClaimsSet claims = jwtService.validateAndParse(token);

            // JIT: Find or create user
            String authProviderId = claims.getSubject();
            String email = claims.getStringClaim("email");

            User user = jitUserService.findOrCreate(authProviderId, email);

            // Store authenticated user in request attribute
            AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getAuthProviderId(),
                user.getEmail()
            );
            request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, authenticatedUser);

            logger.debug("Authenticated user: {} ({})", email, user.getId());

            // Continue filter chain
            filterChain.doFilter(request, response);

        } catch (JwtValidationException e) {
            logger.warn("JWT validation failed for {}: {}", path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid or expired JWT\"}");
        } catch (Exception e) {
            logger.error("Unexpected error during authentication", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Authentication error\"}");
        }
    }

    /**
     * Extract JWT token from Authorization header.
     *
     * @param request the HTTP request
     * @return the JWT token without "Bearer " prefix, or null if not found
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
