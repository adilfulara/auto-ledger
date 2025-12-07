package me.adilfulara.autoledger.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for creating a new fillup.
 */
public record CreateFillupRequest(
        @NotNull(message = "Car ID is required")
        UUID carId,

        @NotNull(message = "Date is required")
        Instant date,

        @NotNull(message = "Odometer reading is required")
        @Positive(message = "Odometer must be positive")
        Long odometer,

        @NotNull(message = "Fuel volume is required")
        @Positive(message = "Fuel volume must be positive")
        BigDecimal fuelVolume,

        @NotNull(message = "Price per unit is required")
        @Positive(message = "Price per unit must be positive")
        BigDecimal pricePerUnit,

        @NotNull(message = "Total cost is required")
        @Positive(message = "Total cost must be positive")
        BigDecimal totalCost,

        Boolean isPartial,

        Boolean isMissed
) {
    /**
     * Returns isPartial with default value of false.
     */
    public boolean isPartialOrDefault() {
        return isPartial != null ? isPartial : false;
    }

    /**
     * Returns isMissed with default value of false.
     */
    public boolean isMissedOrDefault() {
        return isMissed != null ? isMissed : false;
    }
}
