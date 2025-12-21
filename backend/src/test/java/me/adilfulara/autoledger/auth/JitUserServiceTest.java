package me.adilfulara.autoledger.auth;

import me.adilfulara.autoledger.domain.model.User;
import me.adilfulara.autoledger.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JitUserService}.
 * Tests the Just-in-Time user provisioning logic.
 */
@ExtendWith(MockitoExtension.class)
class JitUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JitUserService jitUserService;

    private static final String AUTH_PROVIDER_ID = "clerk_user_123";
    private static final String EMAIL = "test@example.com";
    private static final UUID USER_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    @Test
    void findOrCreate_ExistingUser_ReturnsUser() {
        // Arrange
        User existingUser = new User(USER_ID, AUTH_PROVIDER_ID, EMAIL, null);
        when(userRepository.findByAuthProviderId(AUTH_PROVIDER_ID))
            .thenReturn(Optional.of(existingUser));

        // Act
        User result = jitUserService.findOrCreate(AUTH_PROVIDER_ID, EMAIL);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getAuthProviderId()).isEqualTo(AUTH_PROVIDER_ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL);

        verify(userRepository, times(1)).findByAuthProviderId(AUTH_PROVIDER_ID);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findOrCreate_NewUser_CreatesAndReturnsUser() {
        // Arrange
        User newUser = new User(USER_ID, AUTH_PROVIDER_ID, EMAIL, null);

        when(userRepository.findByAuthProviderId(AUTH_PROVIDER_ID))
            .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
            .thenReturn(newUser);

        // Act
        User result = jitUserService.findOrCreate(AUTH_PROVIDER_ID, EMAIL);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getAuthProviderId()).isEqualTo(AUTH_PROVIDER_ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL);

        verify(userRepository, times(1)).findByAuthProviderId(AUTH_PROVIDER_ID);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void findOrCreate_RaceCondition_HandlesGracefully() {
        // Arrange: First findByAuthProviderId returns empty (user doesn't exist)
        // Then save() throws DataIntegrityViolationException (another thread created it)
        // Second findByAuthProviderId finds the user
        User existingUser = new User(USER_ID, AUTH_PROVIDER_ID, EMAIL, null);

        when(userRepository.findByAuthProviderId(AUTH_PROVIDER_ID))
            .thenReturn(Optional.empty())  // First call: user doesn't exist
            .thenReturn(Optional.of(existingUser));  // Second call: user exists now

        when(userRepository.save(any(User.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // Act
        User result = jitUserService.findOrCreate(AUTH_PROVIDER_ID, EMAIL);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getAuthProviderId()).isEqualTo(AUTH_PROVIDER_ID);

        // Should have tried to find twice (once before save, once in catch block)
        verify(userRepository, times(2)).findByAuthProviderId(AUTH_PROVIDER_ID);
        verify(userRepository, times(1)).save(any(User.class));
    }
}
