package me.adilfulara.autoledger.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UpdateCarRequest Validation")
class UpdateCarRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("rejects year below 1900")
    void rejectsYearBelow1900() {
        var request = new UpdateCarRequest(null, null, 1899, null, null);

        Set<ConstraintViolation<UpdateCarRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("year"));
    }

    @Test
    @DisplayName("rejects year above 2100")
    void rejectsYearAbove2100() {
        var request = new UpdateCarRequest(null, null, 2101, null, null);

        Set<ConstraintViolation<UpdateCarRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("year"));
    }

    @Test
    @DisplayName("accepts null year (partial update)")
    void acceptsNullYear() {
        var request = new UpdateCarRequest("Toyota", null, null, null, null);

        Set<ConstraintViolation<UpdateCarRequest>> violations = validator.validate(request);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("year"));
    }

    @Test
    @DisplayName("accepts valid year")
    void acceptsValidYear() {
        var request = new UpdateCarRequest(null, null, 2020, null, null);

        Set<ConstraintViolation<UpdateCarRequest>> violations = validator.validate(request);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("year"));
    }

    @Test
    @DisplayName("accepts year 1900 (boundary)")
    void acceptsYear1900() {
        var request = new UpdateCarRequest(null, null, 1900, null, null);

        Set<ConstraintViolation<UpdateCarRequest>> violations = validator.validate(request);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("year"));
    }

    @Test
    @DisplayName("accepts year 2100 (boundary)")
    void acceptsYear2100() {
        var request = new UpdateCarRequest(null, null, 2100, null, null);

        Set<ConstraintViolation<UpdateCarRequest>> violations = validator.validate(request);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("year"));
    }
}
