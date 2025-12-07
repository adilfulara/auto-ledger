package me.adilfulara.autoledger.service;

import me.adilfulara.autoledger.api.dto.CarStatsResponse;
import me.adilfulara.autoledger.api.dto.CreateCarRequest;
import me.adilfulara.autoledger.api.dto.UpdateCarRequest;
import me.adilfulara.autoledger.api.exception.ResourceNotFoundException;
import me.adilfulara.autoledger.domain.model.Car;
import me.adilfulara.autoledger.domain.model.DistanceUnit;
import me.adilfulara.autoledger.domain.model.Fillup;
import me.adilfulara.autoledger.domain.model.FuelUnit;
import me.adilfulara.autoledger.domain.repository.CarRepository;
import me.adilfulara.autoledger.domain.repository.FillupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarService")
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private FillupRepository fillupRepository;

    @Mock
    private FillupService fillupService;

    @InjectMocks
    private CarService carService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CAR_ID = UUID.randomUUID();

    private Car createTestCar() {
        Car car = new Car(USER_ID, "Toyota", "Camry", 2022, "1HGBH41JXMN109186", "My Car",
                FuelUnit.GALLONS, DistanceUnit.MILES);
        car.setId(CAR_ID);
        return car;
    }

    @Nested
    @DisplayName("getCarsByUserId")
    class GetCarsByUserId {

        @Test
        @DisplayName("returns cars for user")
        void returnsCarsForUser() {
            Car car = createTestCar();
            when(carRepository.findByUserId(USER_ID)).thenReturn(List.of(car));

            List<Car> result = carService.getCarsByUserId(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMake()).isEqualTo("Toyota");
        }

        @Test
        @DisplayName("returns empty list when no cars")
        void returnsEmptyListWhenNoCars() {
            when(carRepository.findByUserId(USER_ID)).thenReturn(List.of());

            List<Car> result = carService.getCarsByUserId(USER_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCarById")
    class GetCarById {

        @Test
        @DisplayName("returns car when found")
        void returnsCarWhenFound() {
            Car car = createTestCar();
            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(car));

            Car result = carService.getCarById(CAR_ID);

            assertThat(result.getId()).isEqualTo(CAR_ID);
            assertThat(result.getMake()).isEqualTo("Toyota");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsWhenNotFound() {
            when(carRepository.findById(CAR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carService.getCarById(CAR_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Car");
        }
    }

    @Nested
    @DisplayName("createCar")
    class CreateCar {

        @Test
        @DisplayName("creates car with all fields")
        void createsCarWithAllFields() {
            CreateCarRequest request = new CreateCarRequest(
                    "Honda", "Accord", 2023, "VIN123", "Family Car",
                    FuelUnit.LITERS, DistanceUnit.KILOMETERS);

            Car savedCar = new Car(USER_ID, "Honda", "Accord", 2023, "VIN123", "Family Car",
                    FuelUnit.LITERS, DistanceUnit.KILOMETERS);
            savedCar.setId(CAR_ID);

            when(carRepository.save(any(Car.class))).thenReturn(savedCar);

            Car result = carService.createCar(USER_ID, request);

            assertThat(result.getMake()).isEqualTo("Honda");
            assertThat(result.getModel()).isEqualTo("Accord");

            ArgumentCaptor<Car> captor = ArgumentCaptor.forClass(Car.class);
            verify(carRepository).save(captor.capture());
            Car captured = captor.getValue();
            assertThat(captured.getUserId()).isEqualTo(USER_ID);
            assertThat(captured.getFuelUnit()).isEqualTo(FuelUnit.LITERS);
        }
    }

    @Nested
    @DisplayName("updateCar")
    class UpdateCar {

        @Test
        @DisplayName("updates all provided fields")
        void updatesAllProvidedFields() {
            Car existingCar = createTestCar();
            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(existingCar));
            when(carRepository.save(any(Car.class))).thenReturn(existingCar);

            UpdateCarRequest request = new UpdateCarRequest(
                    "Honda", "Civic", 2024, "NEW_VIN", "Updated Name");

            Car result = carService.updateCar(CAR_ID, request);

            assertThat(result.getMake()).isEqualTo("Honda");
            assertThat(result.getModel()).isEqualTo("Civic");
            assertThat(result.getYear()).isEqualTo(2024);
            assertThat(result.getVin()).isEqualTo("NEW_VIN");
            assertThat(result.getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("updates only provided fields")
        void updatesOnlyProvidedFields() {
            Car existingCar = createTestCar();
            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(existingCar));
            when(carRepository.save(any(Car.class))).thenReturn(existingCar);

            UpdateCarRequest request = new UpdateCarRequest("Honda", null, null, null, null);

            Car result = carService.updateCar(CAR_ID, request);

            assertThat(result.getMake()).isEqualTo("Honda");
            assertThat(result.getModel()).isEqualTo("Camry"); // Unchanged
        }

        @Test
        @DisplayName("throws when car not found")
        void throwsWhenNotFound() {
            when(carRepository.findById(CAR_ID)).thenReturn(Optional.empty());

            UpdateCarRequest request = new UpdateCarRequest("Honda", null, null, null, null);

            assertThatThrownBy(() -> carService.updateCar(CAR_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteCar")
    class DeleteCar {

        @Test
        @DisplayName("deletes existing car")
        void deletesExistingCar() {
            when(carRepository.existsById(CAR_ID)).thenReturn(true);

            carService.deleteCar(CAR_ID);

            verify(carRepository).deleteById(CAR_ID);
        }

        @Test
        @DisplayName("throws when car not found")
        void throwsWhenNotFound() {
            when(carRepository.existsById(CAR_ID)).thenReturn(false);

            assertThatThrownBy(() -> carService.deleteCar(CAR_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getCarStats")
    class GetCarStats {

        @Test
        @DisplayName("returns empty stats when no fillups")
        void returnsEmptyStatsWhenNoFillups() {
            Car car = createTestCar();
            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(car));
            when(fillupRepository.findByCarIdOrderByOdometerAsc(CAR_ID)).thenReturn(List.of());

            CarStatsResponse stats = carService.getCarStats(CAR_ID);

            assertThat(stats.carId()).isEqualTo(CAR_ID);
            assertThat(stats.totalFillups()).isZero();
            assertThat(stats.totalDistance()).isZero();
            assertThat(stats.totalFuelUsed()).isEqualTo(BigDecimal.ZERO);
            assertThat(stats.totalSpent()).isEqualTo(BigDecimal.ZERO);
            assertThat(stats.averageMpg()).isNull();
        }

        @Test
        @DisplayName("calculates stats with fillups")
        void calculatesStatsWithFillups() {
            Car car = createTestCar();
            Fillup fillup1 = new Fillup(CAR_ID, Instant.now(), 10000L,
                    new BigDecimal("10.0"), new BigDecimal("3.50"),
                    new BigDecimal("35.00"), false, false);
            fillup1.setId(UUID.randomUUID());

            Fillup fillup2 = new Fillup(CAR_ID, Instant.now(), 10300L,
                    new BigDecimal("15.0"), new BigDecimal("3.60"),
                    new BigDecimal("54.00"), false, false);
            fillup2.setId(UUID.randomUUID());

            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(car));
            when(fillupRepository.findByCarIdOrderByOdometerAsc(CAR_ID)).thenReturn(List.of(fillup1, fillup2));
            when(fillupService.calculateMPG(fillup1)).thenReturn(Optional.empty());
            when(fillupService.calculateMPG(fillup2)).thenReturn(Optional.of(new BigDecimal("20.00")));

            CarStatsResponse stats = carService.getCarStats(CAR_ID);

            assertThat(stats.totalFillups()).isEqualTo(2);
            assertThat(stats.totalDistance()).isEqualTo(300L);
            assertThat(stats.totalFuelUsed()).isEqualByComparingTo(new BigDecimal("25.0"));
            assertThat(stats.totalSpent()).isEqualByComparingTo(new BigDecimal("89.00"));
            assertThat(stats.averageMpg()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(stats.bestMpg()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(stats.worstMpg()).isEqualByComparingTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("calculates best and worst MPG correctly")
        void calculatesBestAndWorstMpg() {
            Car car = createTestCar();
            Fillup fillup1 = new Fillup(CAR_ID, Instant.now(), 10000L,
                    new BigDecimal("10.0"), new BigDecimal("3.50"),
                    new BigDecimal("35.00"), false, false);
            fillup1.setId(UUID.randomUUID());

            Fillup fillup2 = new Fillup(CAR_ID, Instant.now(), 10300L,
                    new BigDecimal("15.0"), new BigDecimal("3.50"),
                    new BigDecimal("52.50"), false, false);
            fillup2.setId(UUID.randomUUID());

            Fillup fillup3 = new Fillup(CAR_ID, Instant.now(), 10600L,
                    new BigDecimal("12.0"), new BigDecimal("3.50"),
                    new BigDecimal("42.00"), false, false);
            fillup3.setId(UUID.randomUUID());

            when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(car));
            when(fillupRepository.findByCarIdOrderByOdometerAsc(CAR_ID)).thenReturn(List.of(fillup1, fillup2, fillup3));
            when(fillupService.calculateMPG(fillup1)).thenReturn(Optional.empty()); // First fillup has no MPG
            when(fillupService.calculateMPG(fillup2)).thenReturn(Optional.of(new BigDecimal("20.00")));
            when(fillupService.calculateMPG(fillup3)).thenReturn(Optional.of(new BigDecimal("25.00")));

            CarStatsResponse stats = carService.getCarStats(CAR_ID);

            assertThat(stats.averageMpg()).isEqualByComparingTo(new BigDecimal("22.50"));
            assertThat(stats.bestMpg()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(stats.worstMpg()).isEqualByComparingTo(new BigDecimal("20.00"));
        }
    }
}
