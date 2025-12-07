package me.adilfulara.autoledger.service;

import me.adilfulara.autoledger.domain.model.Fillup;
import me.adilfulara.autoledger.domain.repository.FillupRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
     * The query is optimized to fetch only the anchor fillup (the most recent non-partial
     * fillup before the current one), avoiding loading all fillups into memory.
     *
     * @param current the fillup to calculate MPG for
     * @return Optional containing the MPG value (scale 2), or empty if MPG cannot be calculated
     * @throws IllegalArgumentException if fuel volume is zero/negative
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

        // Optimized: Single query to find anchor (last full fillup before current)
        Optional<Fillup> anchor = fillupRepository.findLastFullFillupBefore(
                current.getCarId(), current.getOdometer());

        if (anchor.isEmpty()) {
            return Optional.empty();
        }

        // Calculate MPG: (current_odometer - anchor_odometer) / fuel_volume
        long distance = current.getOdometer() - anchor.get().getOdometer();
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
}
