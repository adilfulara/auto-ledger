package me.adilfulara.autoledger.domain.model;

/**
 * Enum representing fuel volume units.
 * Maps to PostgreSQL ENUM type 'fuel_unit'.
 */
public enum FuelUnit {
    /**
     * Gallons (US)
     */
    GALLONS,

    /**
     * Liters (Metric)
     */
    LITERS
}
