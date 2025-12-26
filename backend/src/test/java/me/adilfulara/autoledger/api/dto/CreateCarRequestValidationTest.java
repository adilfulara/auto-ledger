package me.adilfulara.autoledger.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import me.adilfulara.autoledger.domain.model.DistanceUnit;
import me.adilfulara.autoledger.domain.model.FuelUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreateCarRequest Validation")
class CreateCarRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("name field")
    class NameField {
        @Test
        @DisplayName("rejects null name")
        void rejectsNullName() {
            var request = new CreateCarRequest(
                    "Toyota", "Camry", 2020, null, null,
                    FuelUnit.GALLONS, DistanceUnit.MILES);

            Set<ConstraintViolation<CreateCarRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("rejects blank name")
        void rejectsBlankName() {
            var request = new CreateCarRequest(
                    "Toyota", "Camry", 2020, null, "   ",
                    FuelUnit.GALLONS, DistanceUnit.MILES);

            Set<ConstraintViolation<CreateCarRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("rejects name exceeding 100 characters")
        void rejectsNameExceeding100Chars() {
            var longName = "x".repeat(101);
            var request = new CreateCarRequest(
                    "Toyota", "Camry", 2020, null, longName,
                    FuelUnit.GALLONS, DistanceUnit.MILES);

            Set<ConstraintViolation<CreateCarRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("accepts valid name")
        void acceptsValidName() {
            var request = new CreateCarRequest(
                    "Toyota", "Camry", 2020, null, "My Car",
                    FuelUnit.GALLONS, DistanceUnit.MILES);

            Set<ConstraintViolation<CreateCarRequest>> violations = validator.validate(request);

            assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("name"));
        }
    }

    @Nested
    @DisplayName("year field")
    class YearField {
        @Test
        @DisplayName("rejects year below 1900")
        void rejectsYearBelow1900() {
            var request = new CreateCarRequest(
                    "Toyota", "Camry", 1899, null, "Car",
                    FuelUnit.GALLONS, DistanceUnit.MILES);

            Set<ConstraintViolation<CreateCarRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("year"));
        }

        @Test
        @DisplayName("rejects year above 2100")
        void rejectsYearAbove2100() {
            var request = new CreateCarRequest(
                    "Toyota", "Camry", 2101, null, "Car",
                    FuelUnit.GALLONS, DistanceUnit.MILES);

            Set<ConstraintViolation<CreateCarRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("year"));
        }

        @Test
        @DisplayName("accepts year 1900 (boundary)")
        void acceptsYear1900() {
            var request = new CreateCarRequest(
                    "Toyota", "Camry", 1900, null, "Car",
                    FuelUnit.GALLONS, DistanceUnit.MILES);

            Set<ConstraintViolation<CreateCarRequest>> violations = validator.validate(request);

            assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("year"));
        }

        @Test
        @DisplayName("accepts year 2100 (boundary)")
        void acceptsYear2100() {
            var request = new CreateCarRequest(
                    "Toyota", "Camry", 2100, null, "Car",
                    FuelUnit.GALLONS, DistanceUnit.MILES);

            Set<ConstraintViolation<CreateCarRequest>> violations = validator.validate(request);

            assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("year"));
        }
    }
}
