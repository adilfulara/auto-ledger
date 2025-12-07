package me.adilfulara.autoledger.api.controller;

import jakarta.validation.Valid;
import me.adilfulara.autoledger.api.dto.CarResponse;
import me.adilfulara.autoledger.api.dto.CarStatsResponse;
import me.adilfulara.autoledger.api.dto.CreateCarRequest;
import me.adilfulara.autoledger.api.dto.UpdateCarRequest;
import me.adilfulara.autoledger.domain.model.Car;
import me.adilfulara.autoledger.service.CarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Car operations.
 */
@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    /**
     * List all cars for the current user.
     * TODO: Get userId from security context when auth is implemented.
     */
    @GetMapping
    public ResponseEntity<List<CarResponse>> listCars(@RequestParam UUID userId) {
        List<Car> cars = carService.getCarsByUserId(userId);
        List<CarResponse> response = cars.stream()
                .map(CarResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific car by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CarResponse> getCar(@PathVariable UUID id) {
        Car car = carService.getCarById(id);
        return ResponseEntity.ok(CarResponse.from(car));
    }

    /**
     * Create a new car.
     * TODO: Get userId from security context when auth is implemented.
     */
    @PostMapping
    public ResponseEntity<CarResponse> createCar(
            @RequestParam UUID userId,
            @Valid @RequestBody CreateCarRequest request) {
        Car car = carService.createCar(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CarResponse.from(car));
    }

    /**
     * Update an existing car.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CarResponse> updateCar(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCarRequest request) {
        Car car = carService.updateCar(id, request);
        return ResponseEntity.ok(CarResponse.from(car));
    }

    /**
     * Delete a car.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCar(@PathVariable UUID id) {
        carService.deleteCar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get statistics for a car.
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<CarStatsResponse> getCarStats(@PathVariable UUID id) {
        CarStatsResponse stats = carService.getCarStats(id);
        return ResponseEntity.ok(stats);
    }
}
