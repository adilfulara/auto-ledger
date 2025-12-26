package me.adilfulara.autoledger.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing car.
 * Note: fuelUnit and distanceUnit are immutable after creation.
 */
public record UpdateCarRequest(
        @Size(max = 100, message = "Make must be 100 characters or less")
        String make,

        @Size(max = 100, message = "Model must be 100 characters or less")
        String model,

        @Min(value = 1900, message = "Year must be 1900 or later")
        @Max(value = 2100, message = "Year must be 2100 or earlier")
        Integer year,

        @Size(max = 17, message = "VIN must be 17 characters or less")
        String vin,

        @Size(max = 100, message = "Name must be 100 characters or less")
        String name
) {}
