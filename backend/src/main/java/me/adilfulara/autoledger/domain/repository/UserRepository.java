package me.adilfulara.autoledger.domain.repository;

import me.adilfulara.autoledger.domain.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entities.
 * Provides CRUD operations for user management.
 */
@Repository
public interface UserRepository extends CrudRepository<User, UUID> {

    /**
     * Find user by auth provider ID (e.g., Clerk user ID).
     *
     * @param authProviderId the external auth provider ID
     * @return Optional containing the user if found
     */
    Optional<User> findByAuthProviderId(String authProviderId);

    /**
     * Check if a user exists by auth provider ID.
     *
     * @param authProviderId the external auth provider ID
     * @return true if user exists, false otherwise
     */
    boolean existsByAuthProviderId(String authProviderId);

    /**
     * Find user by email address.
     *
     * @param email the email address to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists by email.
     *
     * @param email the email address to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
}
