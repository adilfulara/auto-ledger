package me.adilfulara.autoledger.service;

import me.adilfulara.autoledger.PostgreSQLTestContainer;
import me.adilfulara.autoledger.domain.model.*;
import me.adilfulara.autoledger.domain.repository.CarRepository;
import me.adilfulara.autoledger.domain.repository.FillupRepository;
import me.adilfulara.autoledger.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for FillupService using real PostgreSQL via Testcontainers.
 * Tests the full flow: Service → Repository → Database → Result.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("FillupService Integration Tests")
class FillupServiceIT {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        PostgreSQLTestContainer.configureDataSource(registry);
    }

    @Autowired
    private FillupService fillupService;

    @Autowired
    private FillupRepository fillupRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Car testCar;
    private Instant baseTime;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        fillupRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User("user_integration_test", "integration@test.com");
        testUser = userRepository.save(testUser);

        // Create test car
        testCar = new Car(
                testUser.getId(),
                "Toyota",
                "Camry",
                2022,
                null,
                "Integration Test Car",
                FuelUnit.GALLONS,
                DistanceUnit.MILES
        );
        testCar = carRepository.save(testCar);

        // Base time for creating fillups with proper ordering
        baseTime = Instant.now().minus(30, ChronoUnit.DAYS);
    }

    /**
     * Creates and saves a fillup to the database.
     */
    private Fillup createAndSaveFillup(Long odometer, BigDecimal fuelVolume,
                                        boolean isPartial, boolean isMissed, int daysOffset) {
        Fillup fillup = new Fillup(
                testCar.getId(),
                baseTime.plus(daysOffset, ChronoUnit.DAYS),
                odometer,
                fuelVolume,
                new BigDecimal("3.50"),
                fuelVolume.multiply(new BigDecimal("3.50")),
                isPartial,
                isMissed
        );
        return fillupRepository.save(fillup);
    }

    @Nested
    @DisplayName("calculateMPG with real database")
    class CalculateMPGWithRealDatabase {

        @Test
        @DisplayName("calculates correct MPG for normal fillup sequence")
        void calculatesCorrectMPG_forNormalFillupSequence() {
            // Arrange - create two fillups in database
            Fillup first = createAndSaveFillup(10000L, new BigDecimal("10.0"), false, false, 0);
            Fillup second = createAndSaveFillup(10300L, new BigDecimal("10.0"), false, false, 7);

            // Act
            Optional<BigDecimal> result = fillupService.calculateMPG(second);

            // Assert - 300 miles / 10 gallons = 30 MPG
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("returns empty for first fillup in database")
        void returnsEmpty_forFirstFillupInDatabase() {
            // Arrange - only one fillup exists
            Fillup only = createAndSaveFillup(10000L, new BigDecimal("10.0"), false, false, 0);

            // Act
            Optional<BigDecimal> result = fillupService.calculateMPG(only);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty for partial fillup")
        void returnsEmpty_forPartialFillup() {
            // Arrange
            Fillup first = createAndSaveFillup(10000L, new BigDecimal("10.0"), false, false, 0);
            Fillup partial = createAndSaveFillup(10300L, new BigDecimal("5.0"), true, false, 7);

            // Act
            Optional<BigDecimal> result = fillupService.calculateMPG(partial);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty for missed fillup")
        void returnsEmpty_forMissedFillup() {
            // Arrange
            Fillup first = createAndSaveFillup(10000L, new BigDecimal("10.0"), false, false, 0);
            Fillup missed = createAndSaveFillup(10300L, new BigDecimal("10.0"), false, true, 7);

            // Act
            Optional<BigDecimal> result = fillupService.calculateMPG(missed);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("accumulates fuel from partial fillups in MPG calculation")
        void accumulatesFuel_overPartialFillups() {
            // Arrange - anchor, partial, current
            Fillup anchor = createAndSaveFillup(10000L, new BigDecimal("10.0"), false, false, 0);
            Fillup partial = createAndSaveFillup(10150L, new BigDecimal("5.0"), true, false, 7);
            Fillup current = createAndSaveFillup(10300L, new BigDecimal("10.0"), false, false, 14);

            // Act
            Optional<BigDecimal> result = fillupService.calculateMPG(current);

            // Assert - total fuel = 5.0 + 10.0 = 15.0, MPG = 300 / 15.0 = 20 MPG
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("accumulates fuel from multiple partial fillups")
        void handlesMultiplePartialFillups() {
            // Arrange - anchor, partial1, partial2, current
            Fillup anchor = createAndSaveFillup(10000L, new BigDecimal("10.0"), false, false, 0);
            Fillup partial1 = createAndSaveFillup(10100L, new BigDecimal("3.0"), true, false, 5);
            Fillup partial2 = createAndSaveFillup(10200L, new BigDecimal("4.0"), true, false, 10);
            Fillup current = createAndSaveFillup(10400L, new BigDecimal("10.0"), false, false, 15);

            // Act
            Optional<BigDecimal> result = fillupService.calculateMPG(current);

            // Assert - total fuel = 3.0 + 4.0 + 10.0 = 17.0, MPG = 400 / 17.0 = 23.53 MPG
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo(new BigDecimal("23.53"));
        }

        @Test
        @DisplayName("returns empty when all previous fillups were partial")
        void returnsEmpty_whenAllPreviousWerePartial() {
            // Arrange - no anchor exists
            Fillup partial1 = createAndSaveFillup(10000L, new BigDecimal("5.0"), true, false, 0);
            Fillup partial2 = createAndSaveFillup(10100L, new BigDecimal("5.0"), true, false, 7);
            Fillup current = createAndSaveFillup(10300L, new BigDecimal("10.0"), false, false, 14);

            // Act
            Optional<BigDecimal> result = fillupService.calculateMPG(current);

            // Assert - no anchor point found
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("handles high-precision fuel volumes correctly")
        void handlesHighPrecisionFuelVolumes() {
            // Arrange - use fuel volume with 3 decimal places
            Fillup first = createAndSaveFillup(10000L, new BigDecimal("10.000"), false, false, 0);
            Fillup second = createAndSaveFillup(10285L, new BigDecimal("9.500"), false, false, 7);

            // Act
            Optional<BigDecimal> result = fillupService.calculateMPG(second);

            // Assert - 285 miles / 9.5 gallons = 30 MPG
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("throws exception for zero fuel volume")
        void throwsException_forZeroFuelVolume() {
            // Arrange - create fillup in memory only (not saved, as DB rejects zero fuel)
            Fillup zeroFuel = new Fillup(
                    testCar.getId(),
                    Instant.now(),
                    10300L,
                    BigDecimal.ZERO,  // Zero fuel - validation should catch this
                    new BigDecimal("3.50"),
                    BigDecimal.ZERO,
                    false,
                    false
            );
            zeroFuel.setId(java.util.UUID.randomUUID()); // Set ID to simulate existing record

            // Act & Assert
            assertThatThrownBy(() -> fillupService.calculateMPG(zeroFuel))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fuel volume");
        }
    }
}
