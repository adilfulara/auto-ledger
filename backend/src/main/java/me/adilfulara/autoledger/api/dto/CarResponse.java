package me.adilfulara.autoledger.api.dto;

import me.adilfulara.autoledger.domain.model.Car;
import me.adilfulara.autoledger.domain.model.DistanceUnit;
import me.adilfulara.autoledger.domain.model.FuelUnit;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for car data.
 */
public record CarResponse(
        UUID id,
        String make,
        String model,
        Integer year,
        String vin,
        String name,
        FuelUnit fuelUnit,
        DistanceUnit distanceUnit,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Factory method to create response from domain entity.
     */
    public static CarResponse from(Car car) {
        return new CarResponse(
                car.getId(),
                car.getMake(),
                car.getModel(),
                car.getYear(),
                car.getVin(),
                car.getName(),
                car.getFuelUnit(),
                car.getDistanceUnit(),
                car.getCreatedAt(),
                car.getUpdatedAt()
        );
    }
}
