package me.adilfulara.autoledger.api.controller;

import me.adilfulara.autoledger.PostgreSQLTestContainer;
import me.adilfulara.autoledger.api.dto.*;
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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CarController using real PostgreSQL via Testcontainers.
 * Tests the full HTTP flow: Request → Controller → Service → Repository → Database → Response.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("CarController Integration Tests")
class CarControllerIT {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        PostgreSQLTestContainer.configureDataSource(registry);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private FillupRepository fillupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;

    // Use same UUID as JwtAuthFilter injects when auth is disabled
    private static final UUID TEST_USER_UUID =
        UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    @BeforeEach
    void setUp() {
        fillupRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        // Insert user with specific UUID using JDBC (Spring Data would treat non-null ID as UPDATE)
        jdbcTemplate.update(
            "INSERT INTO app.users (id, auth_provider_id, email, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
            TEST_USER_UUID, "test_user_dev", "dev@test.com"
        );

        // Fetch the created user
        testUser = userRepository.findById(TEST_USER_UUID)
            .orElseThrow(() -> new IllegalStateException("Test user not found"));
    }

    private Car createTestCar(String make, String model) {
        Car car = new Car(testUser.getId(), make, model, 2022, null, "Test Car",
                FuelUnit.GALLONS, DistanceUnit.MILES);
        return carRepository.save(car);
    }

    @Nested
    @DisplayName("GET /api/cars")
    class ListCars {

        @Test
        @DisplayName("returns list of cars for user")
        void returnsListOfCars() {
            Car car = createTestCar("Toyota", "Camry");

            ResponseEntity<List<CarResponse>> response = restTemplate.exchange(
                    "/api/cars?userId=" + testUser.getId(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).make()).isEqualTo("Toyota");
            assertThat(response.getBody().get(0).model()).isEqualTo("Camry");
        }

        @Test
        @DisplayName("returns empty list when no cars")
        void returnsEmptyList() {
            ResponseEntity<List<CarResponse>> response = restTemplate.exchange(
                    "/api/cars?userId=" + testUser.getId(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/cars/{id}")
    class GetCar {

        @Test
        @DisplayName("returns car when found")
        void returnsCarWhenFound() {
            Car car = createTestCar("Honda", "Accord");

            ResponseEntity<CarResponse> response = restTemplate.getForEntity(
                    "/api/cars/" + car.getId(), CarResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().id()).isEqualTo(car.getId());
            assertThat(response.getBody().make()).isEqualTo("Honda");
        }

        @Test
        @DisplayName("returns 404 when not found")
        void returns404WhenNotFound() {
            ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                    "/api/cars/" + UUID.randomUUID(), ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().status()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("POST /api/cars")
    class CreateCar {

        @Test
        @DisplayName("creates car with valid request")
        void createsCarWithValidRequest() {
            CreateCarRequest request = new CreateCarRequest(
                    "Tesla", "Model 3", 2024, "VIN123", "Electric Car",
                    FuelUnit.LITERS, DistanceUnit.KILOMETERS);

            ResponseEntity<CarResponse> response = restTemplate.postForEntity(
                    "/api/cars?userId=" + testUser.getId(), request, CarResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().make()).isEqualTo("Tesla");
            assertThat(response.getBody().model()).isEqualTo("Model 3");
            assertThat(response.getBody().id()).isNotNull();

            // Verify persisted in database
            assertThat(carRepository.findById(response.getBody().id())).isPresent();
        }

        @Test
        @DisplayName("returns 400 for invalid request")
        void returns400ForInvalidRequest() {
            CreateCarRequest request = new CreateCarRequest(
                    "", null, null, null, null, null, null);

            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/api/cars?userId=" + testUser.getId(), request, ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("PUT /api/cars/{id}")
    class UpdateCar {

        @Test
        @DisplayName("updates car with valid request")
        void updatesCarWithValidRequest() {
            Car car = createTestCar("Toyota", "Camry");
            UpdateCarRequest request = new UpdateCarRequest("Toyota", "Corolla", 2025, null, "Updated Name");

            ResponseEntity<CarResponse> response = restTemplate.exchange(
                    "/api/cars/" + car.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(request),
                    CarResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().model()).isEqualTo("Corolla");
            assertThat(response.getBody().year()).isEqualTo(2025);

            // Verify persisted
            Car updated = carRepository.findById(car.getId()).orElseThrow();
            assertThat(updated.getModel()).isEqualTo("Corolla");
        }
    }

    @Nested
    @DisplayName("DELETE /api/cars/{id}")
    class DeleteCar {

        @Test
        @DisplayName("deletes car successfully")
        void deletesCarSuccessfully() {
            Car car = createTestCar("Ford", "Focus");

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/cars/" + car.getId(),
                    HttpMethod.DELETE,
                    null,
                    Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(carRepository.findById(car.getId())).isEmpty();
        }

        @Test
        @DisplayName("returns 404 when car not found")
        void returns404WhenNotFound() {
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    "/api/cars/" + UUID.randomUUID(),
                    HttpMethod.DELETE,
                    null,
                    ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/cars/{id}/stats")
    class GetCarStats {

        @Test
        @DisplayName("returns empty stats when no fillups")
        void returnsEmptyStatsWhenNoFillups() {
            Car car = createTestCar("BMW", "M3");

            ResponseEntity<CarStatsResponse> response = restTemplate.getForEntity(
                    "/api/cars/" + car.getId() + "/stats", CarStatsResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().totalFillups()).isZero();
            assertThat(response.getBody().averageMpg()).isNull();
        }

        @Test
        @DisplayName("returns calculated stats with fillups")
        void returnsCalculatedStatsWithFillups() {
            Car car = createTestCar("Audi", "A4");
            Instant baseTime = Instant.now().minus(30, ChronoUnit.DAYS);

            // Create fillups
            Fillup f1 = new Fillup(car.getId(), baseTime, 10000L,
                    new BigDecimal("10.0"), new BigDecimal("3.50"), new BigDecimal("35.00"), false, false);
            Fillup f2 = new Fillup(car.getId(), baseTime.plus(7, ChronoUnit.DAYS), 10300L,
                    new BigDecimal("10.0"), new BigDecimal("3.60"), new BigDecimal("36.00"), false, false);
            fillupRepository.save(f1);
            fillupRepository.save(f2);

            ResponseEntity<CarStatsResponse> response = restTemplate.getForEntity(
                    "/api/cars/" + car.getId() + "/stats", CarStatsResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().totalFillups()).isEqualTo(2);
            assertThat(response.getBody().totalDistance()).isEqualTo(300L);
            assertThat(response.getBody().averageMpg()).isEqualByComparingTo(new BigDecimal("30.00"));
        }
    }

    @Nested
    @DisplayName("POST /api/cars validation")
    class CreateCarValidation {

        @Test
        @DisplayName("returns 400 with field error when name is null")
        void returns400WhenNameIsNull() {
            var request = new CreateCarRequest(
                    "Toyota", "Camry", 2020, null, null,
                    FuelUnit.GALLONS, DistanceUnit.MILES);

            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/api/cars", request, ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().fieldErrors())
                    .anyMatch(fe -> fe.field().equals("name") && fe.message().contains("required"));
        }

        @Test
        @DisplayName("returns 400 with field error when year is below 1900")
        void returns400WhenYearBelow1900() {
            var request = new CreateCarRequest(
                    "Toyota", "Camry", 1800, null, "My Car",
                    FuelUnit.GALLONS, DistanceUnit.MILES);

            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/api/cars", request, ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().fieldErrors())
                    .anyMatch(fe -> fe.field().equals("year"));
        }

        @Test
        @DisplayName("returns 400 with field error when year is above 2100")
        void returns400WhenYearAbove2100() {
            var request = new CreateCarRequest(
                    "Toyota", "Camry", 3000, null, "Future Car",
                    FuelUnit.GALLONS, DistanceUnit.MILES);

            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/api/cars", request, ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().fieldErrors())
                    .anyMatch(fe -> fe.field().equals("year"));
        }
    }

    @Nested
    @DisplayName("POST /api/cars uniqueness constraints")
    class CreateCarUniqueness {

        @Test
        @DisplayName("returns 409 when creating car with duplicate VIN")
        void returns409ForDuplicateVin() {
            String vin = "1HGBH41JXMN109186";

            // Create first car with VIN
            var request1 = new CreateCarRequest(
                    "Toyota", "Camry", 2020, vin, "Car 1",
                    FuelUnit.GALLONS, DistanceUnit.MILES);
            restTemplate.postForEntity("/api/cars", request1, CarResponse.class);

            // Try to create second car with same VIN
            var request2 = new CreateCarRequest(
                    "Honda", "Civic", 2021, vin, "Car 2",
                    FuelUnit.GALLONS, DistanceUnit.MILES);
            ResponseEntity<ErrorResponse> response =
                    restTemplate.postForEntity("/api/cars", request2, ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().message()).contains("VIN already exists");
        }
    }

    @Nested
    @DisplayName("PUT /api/cars/{id} validation")
    class UpdateCarValidation {

        @Test
        @DisplayName("returns 400 when year is below 1900")
        void returns400WhenYearBelow1900() {
            // Create valid car first
            var createRequest = new CreateCarRequest(
                    "Toyota", "Camry", 2020, null, "My Car",
                    FuelUnit.GALLONS, DistanceUnit.MILES);
            ResponseEntity<CarResponse> createResponse =
                    restTemplate.postForEntity("/api/cars", createRequest, CarResponse.class);
            UUID carId = createResponse.getBody().id();

            // Update with invalid year
            var updateRequest = new UpdateCarRequest(null, null, 1800, null, null);
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    "/api/cars/" + carId, HttpMethod.PUT,
                    new HttpEntity<>(updateRequest), ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().fieldErrors())
                    .anyMatch(fe -> fe.field().equals("year"));
        }

        @Test
        @DisplayName("returns 400 when year is above 2100")
        void returns400WhenYearAbove2100() {
            // Create valid car first
            var createRequest = new CreateCarRequest(
                    "Honda", "Civic", 2021, null, "My Honda",
                    FuelUnit.GALLONS, DistanceUnit.MILES);
            ResponseEntity<CarResponse> createResponse =
                    restTemplate.postForEntity("/api/cars", createRequest, CarResponse.class);
            UUID carId = createResponse.getBody().id();

            // Update with invalid year
            var updateRequest = new UpdateCarRequest(null, null, 3000, null, null);
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    "/api/cars/" + carId, HttpMethod.PUT,
                    new HttpEntity<>(updateRequest), ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().fieldErrors())
                    .anyMatch(fe -> fe.field().equals("year"));
        }
    }
}
