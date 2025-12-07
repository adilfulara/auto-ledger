package me.adilfulara.autoledger.api.dto;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request DTO for updating an existing fillup.
 * Note: carId cannot be changed after creation.
 */
public record UpdateFillupRequest(
        Instant date,

        @Positive(message = "Odometer must be positive")
        Long odometer,

        @Positive(message = "Fuel volume must be positive")
        BigDecimal fuelVolume,

        @Positive(message = "Price per unit must be positive")
        BigDecimal pricePerUnit,

        @Positive(message = "Total cost must be positive")
        BigDecimal totalCost,

        Boolean isPartial,

        Boolean isMissed
) {}
