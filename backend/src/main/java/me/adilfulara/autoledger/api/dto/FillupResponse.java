package me.adilfulara.autoledger.api.dto;

import me.adilfulara.autoledger.domain.model.Fillup;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for fillup data.
 * Includes calculated MPG when available.
 */
public record FillupResponse(
        UUID id,
        UUID carId,
        Instant date,
        Long odometer,
        BigDecimal fuelVolume,
        BigDecimal pricePerUnit,
        BigDecimal totalCost,
        Boolean isPartial,
        Boolean isMissed,
        BigDecimal mpg,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Factory method to create response from domain entity without MPG.
     */
    public static FillupResponse from(Fillup fillup) {
        return from(fillup, null);
    }

    /**
     * Factory method to create response from domain entity with MPG.
     */
    public static FillupResponse from(Fillup fillup, BigDecimal mpg) {
        return new FillupResponse(
                fillup.getId(),
                fillup.getCarId(),
                fillup.getDate(),
                fillup.getOdometer(),
                fillup.getFuelVolume(),
                fillup.getPricePerUnit(),
                fillup.getTotalCost(),
                fillup.getIsPartial(),
                fillup.getIsMissed(),
                mpg,
                fillup.getCreatedAt(),
                fillup.getUpdatedAt()
        );
    }
}
