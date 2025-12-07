package me.adilfulara.autoledger.service;

import me.adilfulara.autoledger.domain.model.Fillup;
import me.adilfulara.autoledger.domain.repository.FillupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FillupService MPG calculation logic.
 * Follows TDD approach - tests written before implementation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FillupService")
class FillupServiceTest {

    @Mock
    private FillupRepository fillupRepository;

    @InjectMocks
    private FillupService fillupService;

    private static final UUID CAR_ID = UUID.randomUUID();

    /**
     * Creates a Fillup with the given parameters for testing.
     */
    private Fillup createFillup(UUID id, Long odometer, BigDecimal fuelVolume,
                                 boolean isPartial, boolean isMissed) {
        Fillup fillup = new Fillup(CAR_ID, Instant.now(), odometer, fuelVolume,
                new BigDecimal("3.50"), new BigDecimal("35.00"), isPartial, isMissed);
        fillup.setId(id);
        return fillup;
    }

    /**
     * Convenience method for creating normal (non-partial, non-missed) fillups.
     */
    private Fillup createNormalFillup(UUID id, Long odometer, BigDecimal fuelVolume) {
        return createFillup(id, odometer, fuelVolume, false, false);
    }

    @Nested
    @DisplayName("calculateMPG")
    class CalculateMPG {

        @Nested
        @DisplayName("when fillup cannot be calculated")
        class WhenCannotCalculate {

            @Test
            @DisplayName("returns empty when this is the first fillup")
            void returnsEmpty_whenFirstFillup() {
                // Arrange
                UUID fillupId = UUID.randomUUID();
                Fillup current = createNormalFillup(fillupId, 10000L, new BigDecimal("10.0"));
                when(fillupRepository.findLastFullFillupBefore(CAR_ID, 10000L))
                        .thenReturn(Optional.empty());

                // Act
                Optional<BigDecimal> result = fillupService.calculateMPG(current);

                // Assert
                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("returns empty when current fillup is partial")
            void returnsEmpty_whenPartialFillup() {
                // Arrange
                UUID fillupId = UUID.randomUUID();
                Fillup current = createFillup(fillupId, 10300L, new BigDecimal("10.0"), true, false);

                // Act
                Optional<BigDecimal> result = fillupService.calculateMPG(current);

                // Assert
                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("returns empty when current fillup is missed")
            void returnsEmpty_whenMissedFillup() {
                // Arrange
                UUID fillupId = UUID.randomUUID();
                Fillup current = createFillup(fillupId, 10300L, new BigDecimal("10.0"), false, true);

                // Act
                Optional<BigDecimal> result = fillupService.calculateMPG(current);

                // Assert
                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("when fillup is normal")
        class WhenNormalFillup {

            @Test
            @DisplayName("calculates correct MPG for normal fillup")
            void returnsMPG_whenNormalFillup() {
                // Arrange
                UUID prevId = UUID.randomUUID();
                UUID currId = UUID.randomUUID();
                Fillup anchor = createNormalFillup(prevId, 10000L, new BigDecimal("10.0"));
                Fillup current = createNormalFillup(currId, 10300L, new BigDecimal("10.0"));

                when(fillupRepository.findLastFullFillupBefore(CAR_ID, 10300L))
                        .thenReturn(Optional.of(anchor));
                when(fillupRepository.sumFuelBetween(CAR_ID, 10000L, 10300L))
                        .thenReturn(new BigDecimal("10.0"));

                // Act
                Optional<BigDecimal> result = fillupService.calculateMPG(current);

                // Assert - 300 miles / 10 gallons = 30 MPG
                assertThat(result).isPresent();
                assertThat(result.get()).isEqualByComparingTo(new BigDecimal("30.00"));
            }

            @ParameterizedTest
            @DisplayName("calculates correct MPG for various distances and fuel volumes")
            @CsvSource({
                    "10000, 10300, 10.0, 30.00",   // 300 miles / 10 gal = 30 MPG
                    "10000, 10400, 10.0, 40.00",   // 400 miles / 10 gal = 40 MPG
                    "10000, 10250, 10.0, 25.00",   // 250 miles / 10 gal = 25 MPG
                    "10000, 10350, 12.5, 28.00"    // 350 miles / 12.5 gal = 28 MPG
            })
            void calculatesCorrectMPG(long anchorOdo, long currOdo, String fuel, String expectedMpg) {
                // Arrange
                UUID anchorId = UUID.randomUUID();
                UUID currId = UUID.randomUUID();
                Fillup anchor = createNormalFillup(anchorId, anchorOdo, new BigDecimal("10.0"));
                Fillup current = createNormalFillup(currId, currOdo, new BigDecimal(fuel));

                when(fillupRepository.findLastFullFillupBefore(CAR_ID, currOdo))
                        .thenReturn(Optional.of(anchor));
                when(fillupRepository.sumFuelBetween(CAR_ID, anchorOdo, currOdo))
                        .thenReturn(new BigDecimal(fuel));

                // Act
                Optional<BigDecimal> result = fillupService.calculateMPG(current);

                // Assert
                assertThat(result).isPresent();
                assertThat(result.get()).isEqualByComparingTo(new BigDecimal(expectedMpg));
            }
        }

        @Nested
        @DisplayName("when previous fillups were partial")
        class WhenPreviousWasPartial {

            @Test
            @DisplayName("accumulates fuel from partial fillups in MPG calculation")
            void accumulatesFuelFromPartials() {
                // Arrange
                // Fillup 1: odometer=10000, isPartial=false (ANCHOR)
                // Fillup 2: odometer=10150, isPartial=true, fuel=5.0
                // Fillup 3: odometer=10300, isPartial=false, fuel=10.0 (CURRENT)
                // Total fuel = 5.0 + 10.0 = 15.0
                // MPG = (10300 - 10000) / 15.0 = 20.0
                UUID anchorId = UUID.randomUUID();
                UUID currId = UUID.randomUUID();

                Fillup anchor = createNormalFillup(anchorId, 10000L, new BigDecimal("10.0"));
                Fillup current = createNormalFillup(currId, 10300L, new BigDecimal("10.0"));

                when(fillupRepository.findLastFullFillupBefore(CAR_ID, 10300L))
                        .thenReturn(Optional.of(anchor));
                // Sum includes partial (5.0) + current (10.0) = 15.0
                when(fillupRepository.sumFuelBetween(CAR_ID, 10000L, 10300L))
                        .thenReturn(new BigDecimal("15.0"));

                // Act
                Optional<BigDecimal> result = fillupService.calculateMPG(current);

                // Assert - 300 miles / 15 gallons = 20 MPG
                assertThat(result).isPresent();
                assertThat(result.get()).isEqualByComparingTo(new BigDecimal("20.00"));
            }

            @Test
            @DisplayName("accumulates fuel from multiple partial fillups")
            void accumulatesFuelFromMultiplePartials() {
                // Arrange
                // Fillup 1: odometer=10000, isPartial=false (ANCHOR)
                // Fillup 2: odometer=10100, isPartial=true, fuel=3.0
                // Fillup 3: odometer=10200, isPartial=true, fuel=4.0
                // Fillup 4: odometer=10400, isPartial=false, fuel=10.0 (CURRENT)
                // Total fuel = 3.0 + 4.0 + 10.0 = 17.0
                // MPG = (10400 - 10000) / 17.0 = 23.53
                UUID anchorId = UUID.randomUUID();
                UUID currId = UUID.randomUUID();

                Fillup anchor = createNormalFillup(anchorId, 10000L, new BigDecimal("10.0"));
                Fillup current = createNormalFillup(currId, 10400L, new BigDecimal("10.0"));

                when(fillupRepository.findLastFullFillupBefore(CAR_ID, 10400L))
                        .thenReturn(Optional.of(anchor));
                // Sum includes partial1 (3.0) + partial2 (4.0) + current (10.0) = 17.0
                when(fillupRepository.sumFuelBetween(CAR_ID, 10000L, 10400L))
                        .thenReturn(new BigDecimal("17.0"));

                // Act
                Optional<BigDecimal> result = fillupService.calculateMPG(current);

                // Assert - 400 miles / 17 gallons = 23.53 MPG
                assertThat(result).isPresent();
                assertThat(result.get()).isEqualByComparingTo(new BigDecimal("23.53"));
            }

            @Test
            @DisplayName("returns empty when no full fillup exists before current")
            void returnsEmpty_whenNoFullFillupExists() {
                // Arrange - query returns empty because all previous fillups were partial
                UUID currId = UUID.randomUUID();
                Fillup current = createNormalFillup(currId, 10300L, new BigDecimal("10.0"));

                when(fillupRepository.findLastFullFillupBefore(CAR_ID, 10300L))
                        .thenReturn(Optional.empty());

                // Act
                Optional<BigDecimal> result = fillupService.calculateMPG(current);

                // Assert - no anchor point, cannot calculate
                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("when validation fails")
        class WhenValidationFails {

            @Test
            @DisplayName("throws exception when fuel volume is zero")
            void throwsException_whenZeroFuelVolume() {
                // Arrange
                UUID fillupId = UUID.randomUUID();
                Fillup current = createNormalFillup(fillupId, 10300L, BigDecimal.ZERO);

                // Act & Assert
                assertThatThrownBy(() -> fillupService.calculateMPG(current))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("fuel volume");
            }

            @Test
            @DisplayName("throws exception when fuel volume is negative")
            void throwsException_whenNegativeFuelVolume() {
                // Arrange
                UUID fillupId = UUID.randomUUID();
                Fillup current = createNormalFillup(fillupId, 10300L, new BigDecimal("-5.0"));

                // Act & Assert
                assertThatThrownBy(() -> fillupService.calculateMPG(current))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("fuel volume");
            }

            @Test
            @DisplayName("returns empty when no fillup exists before current odometer")
            void returnsEmpty_whenNoFillupBeforeCurrent() {
                // Arrange - query returns empty because no fillup exists before current odometer
                UUID currId = UUID.randomUUID();
                Fillup current = createNormalFillup(currId, 10000L, new BigDecimal("10.0"));

                when(fillupRepository.findLastFullFillupBefore(CAR_ID, 10000L))
                        .thenReturn(Optional.empty());

                // Act
                Optional<BigDecimal> result = fillupService.calculateMPG(current);

                // Assert - no previous fillup, returns empty
                assertThat(result).isEmpty();
            }
        }
    }
}
