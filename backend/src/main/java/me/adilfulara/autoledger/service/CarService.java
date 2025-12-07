package me.adilfulara.autoledger.service;

import me.adilfulara.autoledger.api.dto.CarStatsResponse;
import me.adilfulara.autoledger.api.dto.CreateCarRequest;
import me.adilfulara.autoledger.api.dto.UpdateCarRequest;
import me.adilfulara.autoledger.api.exception.ResourceNotFoundException;
import me.adilfulara.autoledger.domain.model.Car;
import me.adilfulara.autoledger.domain.model.Fillup;
import me.adilfulara.autoledger.domain.repository.CarRepository;
import me.adilfulara.autoledger.domain.repository.FillupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Car-related business logic.
 */
@Service
@Transactional
public class CarService {

    private final CarRepository carRepository;
    private final FillupRepository fillupRepository;
    private final FillupService fillupService;

    public CarService(CarRepository carRepository, FillupRepository fillupRepository, FillupService fillupService) {
        this.carRepository = carRepository;
        this.fillupRepository = fillupRepository;
        this.fillupService = fillupService;
    }

    /**
     * Get all cars for a user.
     */
    @Transactional(readOnly = true)
    public List<Car> getCarsByUserId(UUID userId) {
        return carRepository.findByUserId(userId);
    }

    /**
     * Get a car by ID.
     */
    @Transactional(readOnly = true)
    public Car getCarById(UUID carId) {
        return carRepository.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Car", carId));
    }

    /**
     * Create a new car for a user.
     */
    public Car createCar(UUID userId, CreateCarRequest request) {
        Car car = new Car(
                userId,
                request.make(),
                request.model(),
                request.year(),
                request.vin(),
                request.name(),
                request.fuelUnit(),
                request.distanceUnit()
        );
        return carRepository.save(car);
    }

    /**
     * Update an existing car.
     */
    public Car updateCar(UUID carId, UpdateCarRequest request) {
        Car car = getCarById(carId);

        if (request.make() != null) {
            car.setMake(request.make());
        }
        if (request.model() != null) {
            car.setModel(request.model());
        }
        if (request.year() != null) {
            car.setYear(request.year());
        }
        if (request.vin() != null) {
            car.setVin(request.vin());
        }
        if (request.name() != null) {
            car.setName(request.name());
        }

        return carRepository.save(car);
    }

    /**
     * Delete a car and all its fillups.
     */
    public void deleteCar(UUID carId) {
        if (!carRepository.existsById(carId)) {
            throw new ResourceNotFoundException("Car", carId);
        }
        carRepository.deleteById(carId);
    }

    /**
     * Get statistics for a car including MPG metrics.
     */
    @Transactional(readOnly = true)
    public CarStatsResponse getCarStats(UUID carId) {
        Car car = getCarById(carId);
        List<Fillup> fillups = fillupRepository.findByCarIdOrderByOdometerAsc(carId);

        if (fillups.isEmpty()) {
            return new CarStatsResponse(
                    carId,
                    car.getName(),
                    0L,
                    0L,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null,
                    null,
                    null,
                    null
            );
        }

        long totalFillups = fillups.size();
        long totalDistance = fillups.getLast().getOdometer() - fillups.getFirst().getOdometer();
        BigDecimal totalFuelUsed = fillups.stream()
                .map(Fillup::getFuelVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = fillups.stream()
                .map(Fillup::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averagePricePerUnit = fillups.stream()
                .map(Fillup::getPricePerUnit)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(totalFillups), 3, RoundingMode.HALF_UP);

        // Calculate MPG for each fillup
        List<BigDecimal> mpgValues = new ArrayList<>();
        for (Fillup fillup : fillups) {
            Optional<BigDecimal> mpg = fillupService.calculateMPG(fillup);
            mpg.ifPresent(mpgValues::add);
        }

        BigDecimal averageMpg = null;
        BigDecimal bestMpg = null;
        BigDecimal worstMpg = null;

        if (!mpgValues.isEmpty()) {
            averageMpg = mpgValues.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(mpgValues.size()), 2, RoundingMode.HALF_UP);
            bestMpg = mpgValues.stream().max(BigDecimal::compareTo).orElse(null);
            worstMpg = mpgValues.stream().min(BigDecimal::compareTo).orElse(null);
        }

        return new CarStatsResponse(
                carId,
                car.getName(),
                totalFillups,
                totalDistance,
                totalFuelUsed,
                totalSpent,
                averageMpg,
                bestMpg,
                worstMpg,
                averagePricePerUnit
        );
    }
}
