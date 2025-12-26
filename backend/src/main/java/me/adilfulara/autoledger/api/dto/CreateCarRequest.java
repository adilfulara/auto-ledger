package me.adilfulara.autoledger.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import me.adilfulara.autoledger.domain.model.DistanceUnit;
import me.adilfulara.autoledger.domain.model.FuelUnit;

/**
 * Request DTO for creating a new car.
 */
public record CreateCarRequest(
        @NotBlank(message = "Make is required")
        @Size(max = 100, message = "Make must be 100 characters or less")
        String make,

        @NotBlank(message = "Model is required")
        @Size(max = 100, message = "Model must be 100 characters or less")
        String model,

        @NotNull(message = "Year is required")
        @Min(value = 1900, message = "Year must be 1900 or later")
        @Max(value = 2100, message = "Year must be 2100 or earlier")
        Integer year,

        @Size(max = 17, message = "VIN must be 17 characters or less")
        String vin,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be 100 characters or less")
        String name,

        @NotNull(message = "Fuel unit is required")
        FuelUnit fuelUnit,

        @NotNull(message = "Distance unit is required")
        DistanceUnit distanceUnit
) {}
