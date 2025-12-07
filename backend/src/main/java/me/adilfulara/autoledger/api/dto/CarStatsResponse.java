package me.adilfulara.autoledger.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for car statistics including MPG metrics.
 */
public record CarStatsResponse(
        UUID carId,
        String carName,
        Long totalFillups,
        Long totalDistance,
        BigDecimal totalFuelUsed,
        BigDecimal totalSpent,
        BigDecimal averageMpg,
        BigDecimal bestMpg,
        BigDecimal worstMpg,
        BigDecimal averagePricePerUnit
) {}
