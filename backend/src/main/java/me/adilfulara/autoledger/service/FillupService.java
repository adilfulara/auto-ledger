package me.adilfulara.autoledger.service;

import me.adilfulara.autoledger.domain.model.Fillup;
import me.adilfulara.autoledger.domain.repository.FillupRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Service for Fillup-related business logic including MPG calculation.
 */
@Service
public class FillupService {

    private static final int MPG_SCALE = 2;

    private final FillupRepository fillupRepository;

    public FillupService(FillupRepository fillupRepository) {
        this.fillupRepository = fillupRepository;
    }

    /**
     * Calculates the MPG (Miles Per Gallon) for a given fillup.
     * <p>
     * The calculation follows these rules:
     * <ul>
     *   <li>If the current fillup is partial (tank not filled), returns empty</li>
     *   <li>If the current fillup is missed (user missed logging previous), returns empty</li>
     *   <li>If there's no previous full fillup to reference, returns empty</li>
     *   <li>Otherwise, calculates: (current_odometer - anchor_odometer) / fuel_volume</li>
     * </ul>
     * <p>
     * When previous fillups were partial, the algorithm "looks back" to find the last
     * full fillup (anchor point) and uses that for the distance calculation.
     *
     * @param current the fillup to calculate MPG for
     * @return Optional containing the MPG value (scale 2), or empty if MPG cannot be calculated
     * @throws IllegalArgumentException if fuel volume is zero/negative or odometer is invalid
     */
    public Optional<BigDecimal> calculateMPG(Fillup current) {
        // Validate fuel volume
        validateFuelVolume(current.getFuelVolume());

        // Cannot calculate MPG for partial or missed fillups
        if (Boolean.TRUE.equals(current.getIsPartial())) {
            return Optional.empty();
        }
        if (Boolean.TRUE.equals(current.getIsMissed())) {
            return Optional.empty();
        }

        // Get all fillups for this car ordered by odometer
        List<Fillup> fillups = fillupRepository.findByCarIdOrderByOdometerAsc(current.getCarId());

        // Find the current fillup's position in the list
        int currentIndex = findFillupIndex(fillups, current);

        // Cannot calculate if this is the first fillup
        if (currentIndex <= 0) {
            return Optional.empty();
        }

        // Find the anchor point - walk backwards to find the last full fillup
        Fillup anchor = findAnchorFillup(fillups, currentIndex);
        if (anchor == null) {
            return Optional.empty();
        }

        // Calculate MPG: (current_odometer - anchor_odometer) / fuel_volume
        long distance = current.getOdometer() - anchor.getOdometer();
        BigDecimal mpg = BigDecimal.valueOf(distance)
                .divide(current.getFuelVolume(), MPG_SCALE, RoundingMode.HALF_UP);

        return Optional.of(mpg);
    }

    /**
     * Validates that fuel volume is positive.
     */
    private void validateFuelVolume(BigDecimal fuelVolume) {
        if (fuelVolume == null || fuelVolume.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("fuel volume must be positive");
        }
    }

    /**
     * Finds the index of the given fillup in the list.
     * Returns -1 if not found.
     */
    private int findFillupIndex(List<Fillup> fillups, Fillup target) {
        for (int i = 0; i < fillups.size(); i++) {
            if (fillups.get(i).getId().equals(target.getId())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the anchor fillup - the most recent non-partial fillup before the current one.
     * This is used as the reference point for MPG calculation.
     *
     * @param fillups list of fillups ordered by odometer
     * @param currentIndex index of the current fillup
     * @return the anchor fillup, or null if no valid anchor exists
     */
    private Fillup findAnchorFillup(List<Fillup> fillups, int currentIndex) {
        for (int i = currentIndex - 1; i >= 0; i--) {
            Fillup candidate = fillups.get(i);
            // A valid anchor is a non-partial fillup
            if (!Boolean.TRUE.equals(candidate.getIsPartial())) {
                return candidate;
            }
        }
        return null;
    }
}
