package me.adilfulara.autoledger.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.adilfulara.autoledger.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthFilter using mocked dependencies.
 * Tests all authentication scenarios without Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter Unit Tests")
class JwtAuthFilterTest {

    @Mock
    private AuthProperties authProperties;

    @Mock
    private JwtService jwtService;

    @Mock
    private JitUserService jitUserService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter jwtAuthFilter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() {
        jwtAuthFilter = new JwtAuthFilter(authProperties, jwtService, jitUserService);
    }

    /**
     * Helper method to setup response writer for tests that need to capture JSON responses.
     */
    private void setupResponseWriter() throws Exception {
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Nested
    @DisplayName("When authentication is disabled")
    class WhenAuthDisabled {

        @Test
        @DisplayName("should inject test user and continue filter chain")
        void shouldInjectTestUserAndContinue() throws Exception {
            // Given
            when(authProperties.isEnabled()).thenReturn(false);

            // When
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Then
            ArgumentCaptor<AuthenticatedUser> userCaptor = ArgumentCaptor.forClass(AuthenticatedUser.class);
            verify(request).setAttribute(eq(JwtAuthFilter.AUTHENTICATED_USER_ATTRIBUTE), userCaptor.capture());

            AuthenticatedUser capturedUser = userCaptor.getValue();
            assertThat(capturedUser.userId()).isEqualTo(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));
            assertThat(capturedUser.authProviderId()).isEqualTo("test_user_dev");
            assertThat(capturedUser.email()).isEqualTo("dev@test.com");

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService, jitUserService);
        }
    }

    @Nested
    @DisplayName("When accessing health check endpoints")
    class WhenHealthCheckEndpoints {

        @BeforeEach
        void setUp() {
            when(authProperties.isEnabled()).thenReturn(true);
        }

        @Test
        @DisplayName("should skip authentication for /actuator/health")
        void shouldSkipAuthForHealthEndpoint() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/actuator/health");

            // When
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService, jitUserService);
            verify(request, never()).setAttribute(anyString(), any());
        }

        @Test
        @DisplayName("should skip authentication for /actuator/info")
        void shouldSkipAuthForInfoEndpoint() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/actuator/info");

            // When
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService, jitUserService);
        }
    }

    @Nested
    @DisplayName("When Authorization header is missing or invalid")
    class WhenMissingOrInvalidHeader {

        @BeforeEach
        void setUp() {
            when(authProperties.isEnabled()).thenReturn(true);
            when(request.getRequestURI()).thenReturn("/api/cars");
        }

        @Test
        @DisplayName("should return 401 when Authorization header is missing")
        void shouldReturn401WhenHeaderMissing() throws Exception {
            // Given
            setupResponseWriter();
            when(request.getHeader("Authorization")).thenReturn(null);

            // When
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response).setContentType("application/json");
            assertThat(responseWriter.toString()).contains("Missing or invalid Authorization header");
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("should return 401 when Authorization header does not start with 'Bearer '")
        void shouldReturn401WhenNotBearerToken() throws Exception {
            // Given
            setupResponseWriter();
            when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

            // When
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            assertThat(responseWriter.toString()).contains("Missing or invalid Authorization header");
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("When JWT token is present")
    class WhenJwtTokenPresent {

        private static final String VALID_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...";
        private static final UUID USER_ID = UUID.randomUUID();

        @BeforeEach
        void setUp() {
            when(authProperties.isEnabled()).thenReturn(true);
            when(request.getRequestURI()).thenReturn("/api/cars");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        }

        @Test
        @DisplayName("should authenticate and continue filter chain on valid JWT")
        void shouldAuthenticateOnValidJwt() throws Exception {
            // Given
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("auth_provider_123")
                .claim("email", "user@example.com")
                .build();

            User mockUser = new User("auth_provider_123", "user@example.com");
            mockUser.setId(USER_ID);

            when(jwtService.validateAndParse(VALID_TOKEN)).thenReturn(claims);
            when(jitUserService.findOrCreate("auth_provider_123", "user@example.com")).thenReturn(mockUser);

            // When
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Then
            ArgumentCaptor<AuthenticatedUser> userCaptor = ArgumentCaptor.forClass(AuthenticatedUser.class);
            verify(request).setAttribute(eq(JwtAuthFilter.AUTHENTICATED_USER_ATTRIBUTE), userCaptor.capture());

            AuthenticatedUser capturedUser = userCaptor.getValue();
            assertThat(capturedUser.userId()).isEqualTo(USER_ID);
            assertThat(capturedUser.authProviderId()).isEqualTo("auth_provider_123");
            assertThat(capturedUser.email()).isEqualTo("user@example.com");

            verify(filterChain).doFilter(request, response);
            verify(jwtService).validateAndParse(VALID_TOKEN);
            verify(jitUserService).findOrCreate("auth_provider_123", "user@example.com");
        }

        @Test
        @DisplayName("should return 401 when JWT validation fails")
        void shouldReturn401OnJwtValidationFailure() throws Exception {
            // Given
            setupResponseWriter();
            when(jwtService.validateAndParse(VALID_TOKEN))
                .thenThrow(new JwtValidationException("Token expired"));

            // When
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response).setContentType("application/json");
            assertThat(responseWriter.toString()).contains("Invalid or expired JWT");
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("should return 500 when unexpected exception occurs")
        void shouldReturn500OnUnexpectedException() throws Exception {
            // Given
            setupResponseWriter();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("auth_provider_123")
                .claim("email", "user@example.com")
                .build();

            when(jwtService.validateAndParse(VALID_TOKEN)).thenReturn(claims);
            when(jitUserService.findOrCreate(anyString(), anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));

            // When
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            verify(response).setContentType("application/json");
            assertThat(responseWriter.toString()).contains("Authentication error");
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("Token extraction")
    class TokenExtraction {

        @BeforeEach
        void setUp() {
            when(authProperties.isEnabled()).thenReturn(true);
            when(request.getRequestURI()).thenReturn("/api/cars");
        }

        @Test
        @DisplayName("should extract token from valid Bearer header")
        void shouldExtractTokenFromValidBearerHeader() throws Exception {
            // Given
            String token = "valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("user_123")
                .claim("email", "test@example.com")
                .build();

            User mockUser = new User("user_123", "test@example.com");
            mockUser.setId(UUID.randomUUID());

            when(jwtService.validateAndParse(token)).thenReturn(claims);
            when(jitUserService.findOrCreate(anyString(), anyString())).thenReturn(mockUser);

            // When
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtService).validateAndParse(token);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should handle empty Bearer header gracefully")
        void shouldHandleEmptyBearerHeader() throws Exception {
            // Given
            setupResponseWriter();
            when(request.getHeader("Authorization")).thenReturn("Bearer ");
            when(jwtService.validateAndParse(""))
                .thenThrow(new JwtValidationException("Malformed JWT"));

            // When
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // Then
            // Empty token will fail validation, returning 401
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response).setContentType("application/json");
            assertThat(responseWriter.toString()).contains("Invalid or expired JWT");
        }
    }
}
