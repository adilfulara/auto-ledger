package me.adilfulara.autoledger.api.exception;

/**
 * Exception thrown when odometer validation fails.
 */
public class InvalidOdometerException extends RuntimeException {

    public InvalidOdometerException(String message) {
        super(message);
    }

    public InvalidOdometerException(Long newOdometer, Long previousOdometer) {
        super(String.format("Odometer reading %d must be greater than previous reading %d",
                newOdometer, previousOdometer));
    }
}
