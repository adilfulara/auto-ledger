package me.adilfulara.autoledger.domain.repository;

import me.adilfulara.autoledger.domain.model.Fillup;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Fillup entities.
 * Provides CRUD operations for fillup management and MPG calculations.
 */
@Repository
public interface FillupRepository extends CrudRepository<Fillup, UUID> {

    /**
     * Find all fillups for a specific car, ordered by date descending.
     *
     * @param carId the car's UUID
     * @return list of fillups ordered by date (newest first)
     */
    @Query("SELECT * FROM fillups WHERE car_id = :carId ORDER BY date DESC")
    List<Fillup> findByCarIdOrderByDateDesc(@Param("carId") UUID carId);

    /**
     * Find the last N fillups for a specific car (for MCP history resource).
     *
     * @param carId the car's UUID
     * @param limit maximum number of records to return
     * @return list of recent fillups
     */
    @Query("SELECT * FROM fillups WHERE car_id = :carId ORDER BY date DESC LIMIT :limit")
    List<Fillup> findRecentByCarId(@Param("carId") UUID carId, @Param("limit") int limit);

    /**
     * Find the most recent fillup for a car (to validate odometer progression).
     *
     * @param carId the car's UUID
     * @return Optional containing the most recent fillup if it exists
     */
    @Query("SELECT * FROM fillups WHERE car_id = :carId ORDER BY date DESC LIMIT 1")
    Optional<Fillup> findMostRecentByCarId(@Param("carId") UUID carId);

    /**
     * Find all fillups for a car ordered by odometer ascending.
     * Useful for MPG calculations.
     *
     * @param carId the car's UUID
     * @return list of fillups ordered by odometer
     */
    @Query("SELECT * FROM fillups WHERE car_id = :carId ORDER BY odometer ASC")
    List<Fillup> findByCarIdOrderByOdometerAsc(@Param("carId") UUID carId);

    /**
     * Count fillups for a specific car.
     *
     * @param carId the car's UUID
     * @return number of fillups recorded
     */
    long countByCarId(UUID carId);
}
