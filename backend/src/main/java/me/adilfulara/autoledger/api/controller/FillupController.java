package me.adilfulara.autoledger.api.controller;

import jakarta.validation.Valid;
import me.adilfulara.autoledger.api.dto.CreateFillupRequest;
import me.adilfulara.autoledger.api.dto.FillupResponse;
import me.adilfulara.autoledger.api.dto.UpdateFillupRequest;
import me.adilfulara.autoledger.api.exception.InvalidOdometerException;
import me.adilfulara.autoledger.api.exception.ResourceNotFoundException;
import me.adilfulara.autoledger.domain.model.Fillup;
import me.adilfulara.autoledger.domain.repository.CarRepository;
import me.adilfulara.autoledger.domain.repository.FillupRepository;
import me.adilfulara.autoledger.service.FillupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for Fillup operations.
 */
@RestController
@RequestMapping("/api")
public class FillupController {

    private final FillupRepository fillupRepository;
    private final CarRepository carRepository;
    private final FillupService fillupService;

    public FillupController(FillupRepository fillupRepository, CarRepository carRepository,
                            FillupService fillupService) {
        this.fillupRepository = fillupRepository;
        this.carRepository = carRepository;
        this.fillupService = fillupService;
    }

    /**
     * Get a specific fillup by ID.
     */
    @GetMapping("/fillups/{id}")
    public ResponseEntity<FillupResponse> getFillup(@PathVariable UUID id) {
        Fillup fillup = fillupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fillup", id));
        Optional<BigDecimal> mpg = fillupService.calculateMPG(fillup);
        return ResponseEntity.ok(FillupResponse.from(fillup, mpg.orElse(null)));
    }

    /**
     * Create a new fillup.
     */
    @PostMapping("/fillups")
    public ResponseEntity<FillupResponse> createFillup(@Valid @RequestBody CreateFillupRequest request) {
        // Verify car exists
        if (!carRepository.existsById(request.carId())) {
            throw new ResourceNotFoundException("Car", request.carId());
        }

        // Validate odometer is greater than previous
        Optional<Fillup> mostRecent = fillupRepository.findMostRecentByCarId(request.carId());
        if (mostRecent.isPresent() && request.odometer() <= mostRecent.get().getOdometer()) {
            throw new InvalidOdometerException(request.odometer(), mostRecent.get().getOdometer());
        }

        Fillup fillup = new Fillup(
                request.carId(),
                request.date(),
                request.odometer(),
                request.fuelVolume(),
                request.pricePerUnit(),
                request.totalCost(),
                request.isPartialOrDefault(),
                request.isMissedOrDefault()
        );

        Fillup saved = fillupRepository.save(fillup);
        Optional<BigDecimal> mpg = fillupService.calculateMPG(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(FillupResponse.from(saved, mpg.orElse(null)));
    }

    /**
     * Update an existing fillup.
     */
    @PutMapping("/fillups/{id}")
    public ResponseEntity<FillupResponse> updateFillup(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFillupRequest request) {
        Fillup fillup = fillupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fillup", id));

        if (request.date() != null) {
            fillup.setDate(request.date());
        }
        if (request.odometer() != null) {
            fillup.setOdometer(request.odometer());
        }
        if (request.fuelVolume() != null) {
            fillup.setFuelVolume(request.fuelVolume());
        }
        if (request.pricePerUnit() != null) {
            fillup.setPricePerUnit(request.pricePerUnit());
        }
        if (request.totalCost() != null) {
            fillup.setTotalCost(request.totalCost());
        }
        if (request.isPartial() != null) {
            fillup.setIsPartial(request.isPartial());
        }
        if (request.isMissed() != null) {
            fillup.setIsMissed(request.isMissed());
        }

        Fillup saved = fillupRepository.save(fillup);
        Optional<BigDecimal> mpg = fillupService.calculateMPG(saved);
        return ResponseEntity.ok(FillupResponse.from(saved, mpg.orElse(null)));
    }

    /**
     * Delete a fillup.
     */
    @DeleteMapping("/fillups/{id}")
    public ResponseEntity<Void> deleteFillup(@PathVariable UUID id) {
        if (!fillupRepository.existsById(id)) {
            throw new ResourceNotFoundException("Fillup", id);
        }
        fillupRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all fillups for a specific car.
     */
    @GetMapping("/cars/{carId}/fillups")
    public ResponseEntity<List<FillupResponse>> getFillupsByCarId(@PathVariable UUID carId) {
        if (!carRepository.existsById(carId)) {
            throw new ResourceNotFoundException("Car", carId);
        }
        List<Fillup> fillups = fillupRepository.findByCarIdOrderByDateDesc(carId);
        List<FillupResponse> response = fillups.stream()
                .map(f -> {
                    Optional<BigDecimal> mpg = fillupService.calculateMPG(f);
                    return FillupResponse.from(f, mpg.orElse(null));
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get recent fillups for a car (last 50 for trend analysis).
     */
    @GetMapping("/cars/{carId}/fillups/recent")
    public ResponseEntity<List<FillupResponse>> getRecentFillups(
            @PathVariable UUID carId,
            @RequestParam(defaultValue = "50") int limit) {
        if (!carRepository.existsById(carId)) {
            throw new ResourceNotFoundException("Car", carId);
        }
        List<Fillup> fillups = fillupRepository.findRecentByCarId(carId, Math.min(limit, 50));
        List<FillupResponse> response = fillups.stream()
                .map(f -> {
                    Optional<BigDecimal> mpg = fillupService.calculateMPG(f);
                    return FillupResponse.from(f, mpg.orElse(null));
                })
                .toList();
        return ResponseEntity.ok(response);
    }
}
