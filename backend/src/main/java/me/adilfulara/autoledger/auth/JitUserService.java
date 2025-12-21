package me.adilfulara.autoledger.auth;

import me.adilfulara.autoledger.domain.model.User;
import me.adilfulara.autoledger.domain.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Just-in-Time (JIT) user provisioning.
 *
 * <p>When an authenticated user makes their first API request, this service creates
 * a database record for them automatically. Subsequent requests find the existing user.
 */
@Service
public class JitUserService {

    private final UserRepository userRepository;

    public JitUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Find an existing user or create a new one based on auth provider ID.
     *
     * <p>Thread-safe with race condition handling:
     * If two requests for the same new user arrive simultaneously, one will create
     * the user and the other will catch the duplicate key exception and fetch the
     * user created by the first thread.
     *
     * @param authProviderId the external auth provider ID (JWT 'sub' claim)
     * @param email the user's email address
     * @return the existing or newly created User entity
     */
    @Transactional
    public User findOrCreate(String authProviderId, String email) {
        return userRepository.findByAuthProviderId(authProviderId)
            .orElseGet(() -> {
                try {
                    return userRepository.save(new User(authProviderId, email));
                } catch (DataIntegrityViolationException e) {
                    // Race condition: another thread created the user
                    // Fetch it from the database
                    return userRepository.findByAuthProviderId(authProviderId)
                        .orElseThrow(() -> new IllegalStateException(
                            "Failed to create or find user with authProviderId: " + authProviderId, e));
                }
            });
    }
}
