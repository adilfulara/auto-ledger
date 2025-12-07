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
     *   <li>Otherwise, calculates: (current_odometer - anchor_odometer) / total_fuel</li>
     * </ul>
     * <p>
     * The total fuel includes all fillups between the anchor and current (inclusive of current,
     * exclusive of anchor). This correctly handles partial fillups by accumulating their fuel.
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

        // Find anchor (last full fillup before current)
        Optional<Fillup> anchor = fillupRepository.findLastFullFillupBefore(
                current.getCarId(), current.getOdometer());

        if (anchor.isEmpty()) {
            return Optional.empty();
        }

        // Sum all fuel between anchor and current (handles partial fillups)
        BigDecimal totalFuel = fillupRepository.sumFuelBetween(
                current.getCarId(), anchor.get().getOdometer(), current.getOdometer());

        // Calculate MPG: (current_odometer - anchor_odometer) / total_fuel
        long distance = current.getOdometer() - anchor.get().getOdometer();
        BigDecimal mpg = BigDecimal.valueOf(distance)
                .divide(totalFuel, MPG_SCALE, RoundingMode.HALF_UP);

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
