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
            @DisplayName("skips partial fillups and uses last full fillup as anchor")
            void usesLastFullFillupAsAnchor_whenPreviousWasPartial() {
                // Arrange
                // Database query returns the anchor directly, skipping partial fillups
                // Fillup 1: odometer=10000, isPartial=false (ANCHOR - returned by query)
                // Fillup 2: odometer=10150, isPartial=true  (SKIPPED by query)
                // Fillup 3: odometer=10300, isPartial=false (CURRENT)
                // MPG = (10300 - 10000) / 10.0 = 30.0
                UUID anchorId = UUID.randomUUID();
                UUID currId = UUID.randomUUID();

                Fillup anchor = createNormalFillup(anchorId, 10000L, new BigDecimal("10.0"));
                Fillup current = createNormalFillup(currId, 10300L, new BigDecimal("10.0"));

                // The optimized query returns only the anchor, skipping partials
                when(fillupRepository.findLastFullFillupBefore(CAR_ID, 10300L))
                        .thenReturn(Optional.of(anchor));

                // Act
                Optional<BigDecimal> result = fillupService.calculateMPG(current);

                // Assert - 300 miles / 10 gallons = 30 MPG
                assertThat(result).isPresent();
                assertThat(result.get()).isEqualByComparingTo(new BigDecimal("30.00"));
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
