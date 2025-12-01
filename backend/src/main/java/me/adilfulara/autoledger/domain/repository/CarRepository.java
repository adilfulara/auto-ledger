package me.adilfulara.autoledger.domain.repository;

import me.adilfulara.autoledger.domain.model.Car;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Car entities.
 * Provides CRUD operations for car management.
 */
@Repository
public interface CarRepository extends CrudRepository<Car, UUID> {

    /**
     * Find all cars belonging to a specific user.
     *
     * @param userId the user's internal UUID
     * @return list of cars owned by the user
     */
    List<Car> findByUserId(UUID userId);

    /**
     * Find a car by VIN.
     *
     * @param vin the Vehicle Identification Number
     * @return Optional containing the car if found
     */
    Optional<Car> findByVin(String vin);

    /**
     * Find a car by user ID and name (for fuzzy matching in MCP tools).
     *
     * @param userId the user's internal UUID
     * @param name the car name
     * @return Optional containing the car if found
     */
    Optional<Car> findByUserIdAndName(UUID userId, String name);

    /**
     * Count cars belonging to a specific user.
     *
     * @param userId the user's internal UUID
     * @return number of cars owned by the user
     */
    long countByUserId(UUID userId);
}
