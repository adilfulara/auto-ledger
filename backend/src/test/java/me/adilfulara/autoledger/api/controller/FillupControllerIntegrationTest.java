package me.adilfulara.autoledger.api.controller;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for FillupController using real PostgreSQL via Testcontainers.
 * Tests the full HTTP flow: Request → Controller → Service → Repository → Database → Response.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("FillupController Integration Tests")
class FillupControllerIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("autoledger_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private FillupRepository fillupRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Car testCar;
    private Instant baseTime;

    @BeforeEach
    void setUp() {
        fillupRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("user_fillup_api_test", "fillupapi@test.com");
        testUser = userRepository.save(testUser);

        testCar = new Car(testUser.getId(), "Toyota", "Camry", 2022, null, "Test Car",
                FuelUnit.GALLONS, DistanceUnit.MILES);
        testCar = carRepository.save(testCar);

        baseTime = Instant.now().minus(30, ChronoUnit.DAYS);
    }

    private Fillup createTestFillup(Long odometer, int daysOffset) {
        Fillup fillup = new Fillup(testCar.getId(), baseTime.plus(daysOffset, ChronoUnit.DAYS),
                odometer, new BigDecimal("10.0"), new BigDecimal("3.50"),
                new BigDecimal("35.00"), false, false);
        return fillupRepository.save(fillup);
    }

    @Nested
    @DisplayName("GET /api/fillups/{id}")
    class GetFillup {

        @Test
        @DisplayName("returns fillup with computed MPG")
        void returnsFillupWithMPG() {
            Fillup first = createTestFillup(10000L, 0);
            Fillup second = createTestFillup(10300L, 7);

            ResponseEntity<FillupResponse> response = restTemplate.getForEntity(
                    "/api/fillups/" + second.getId(), FillupResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().id()).isEqualTo(second.getId());
            assertThat(response.getBody().mpg()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("returns 404 when not found")
        void returns404WhenNotFound() {
            ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                    "/api/fillups/" + UUID.randomUUID(), ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("POST /api/fillups")
    class CreateFillup {

        @Test
        @DisplayName("creates fillup with valid request")
        void createsFillupWithValidRequest() {
            CreateFillupRequest request = new CreateFillupRequest(
                    testCar.getId(), Instant.now(), 10000L,
                    new BigDecimal("12.5"), new BigDecimal("3.75"),
                    new BigDecimal("46.88"), false, false);

            ResponseEntity<FillupResponse> response = restTemplate.postForEntity(
                    "/api/fillups", request, FillupResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().carId()).isEqualTo(testCar.getId());
            assertThat(response.getBody().odometer()).isEqualTo(10000L);
            assertThat(response.getBody().id()).isNotNull();

            // Verify persisted
            assertThat(fillupRepository.findById(response.getBody().id())).isPresent();
        }

        @Test
        @DisplayName("returns 404 when car not found")
        void returns404WhenCarNotFound() {
            CreateFillupRequest request = new CreateFillupRequest(
                    UUID.randomUUID(), Instant.now(), 10000L,
                    new BigDecimal("10.0"), new BigDecimal("3.50"),
                    new BigDecimal("35.00"), false, false);

            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/api/fillups", request, ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("returns 400 when odometer not greater than previous")
        void returns400WhenOdometerInvalid() {
            createTestFillup(15000L, 0);

            CreateFillupRequest request = new CreateFillupRequest(
                    testCar.getId(), Instant.now(), 10000L,
                    new BigDecimal("10.0"), new BigDecimal("3.50"),
                    new BigDecimal("35.00"), false, false);

            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    "/api/fillups", request, ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).containsIgnoringCase("odometer");
        }
    }

    @Nested
    @DisplayName("PUT /api/fillups/{id}")
    class UpdateFillup {

        @Test
        @DisplayName("updates fillup with valid request")
        void updatesFillupWithValidRequest() {
            Fillup fillup = createTestFillup(10000L, 0);
            UpdateFillupRequest request = new UpdateFillupRequest(
                    null, 10500L, new BigDecimal("15.0"), null, null, null, null);

            ResponseEntity<FillupResponse> response = restTemplate.exchange(
                    "/api/fillups/" + fillup.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(request),
                    FillupResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().odometer()).isEqualTo(10500L);

            // Verify persisted
            Fillup updated = fillupRepository.findById(fillup.getId()).orElseThrow();
            assertThat(updated.getOdometer()).isEqualTo(10500L);
            assertThat(updated.getFuelVolume()).isEqualByComparingTo(new BigDecimal("15.0"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/fillups/{id}")
    class DeleteFillup {

        @Test
        @DisplayName("deletes fillup successfully")
        void deletesFillupSuccessfully() {
            Fillup fillup = createTestFillup(10000L, 0);

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/fillups/" + fillup.getId(),
                    HttpMethod.DELETE,
                    null,
                    Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(fillupRepository.findById(fillup.getId())).isEmpty();
        }

        @Test
        @DisplayName("returns 404 when not found")
        void returns404WhenNotFound() {
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    "/api/fillups/" + UUID.randomUUID(),
                    HttpMethod.DELETE,
                    null,
                    ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/cars/{carId}/fillups")
    class GetFillupsByCarId {

        @Test
        @DisplayName("returns fillups for car with MPG")
        void returnsFillupsByCarId() {
            Fillup first = createTestFillup(10000L, 0);
            Fillup second = createTestFillup(10300L, 7);

            ResponseEntity<List<FillupResponse>> response = restTemplate.exchange(
                    "/api/cars/" + testCar.getId() + "/fillups",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            // Results ordered by date desc, so second fillup is first
            assertThat(response.getBody().get(0).odometer()).isEqualTo(10300L);
            assertThat(response.getBody().get(0).mpg()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("returns 404 when car not found")
        void returns404WhenCarNotFound() {
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    "/api/cars/" + UUID.randomUUID() + "/fillups",
                    HttpMethod.GET,
                    null,
                    ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/cars/{carId}/fillups/recent")
    class GetRecentFillups {

        @Test
        @DisplayName("returns recent fillups with default limit")
        void returnsRecentFillups() {
            for (int i = 0; i < 5; i++) {
                createTestFillup(10000L + (i * 100), i);
            }

            ResponseEntity<List<FillupResponse>> response = restTemplate.exchange(
                    "/api/cars/" + testCar.getId() + "/fillups/recent",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(5);
        }

        @Test
        @DisplayName("respects custom limit parameter")
        void respectsCustomLimit() {
            for (int i = 0; i < 10; i++) {
                createTestFillup(10000L + (i * 100), i);
            }

            ResponseEntity<List<FillupResponse>> response = restTemplate.exchange(
                    "/api/cars/" + testCar.getId() + "/fillups/recent?limit=3",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("MPG Calculation via API")
    class MpgCalculationViaApi {

        @Test
        @DisplayName("MPG accumulates fuel from partial fillups")
        void mpgAccumulatesFuelFromPartials() {
            // Create anchor fillup
            Fillup anchor = new Fillup(testCar.getId(), baseTime, 10000L,
                    new BigDecimal("10.0"), new BigDecimal("3.50"), new BigDecimal("35.00"), false, false);
            fillupRepository.save(anchor);

            // Create partial fillup
            Fillup partial = new Fillup(testCar.getId(), baseTime.plus(7, ChronoUnit.DAYS), 10150L,
                    new BigDecimal("5.0"), new BigDecimal("3.50"), new BigDecimal("17.50"), true, false);
            fillupRepository.save(partial);

            // Create current fillup via API
            CreateFillupRequest request = new CreateFillupRequest(
                    testCar.getId(), baseTime.plus(14, ChronoUnit.DAYS), 10300L,
                    new BigDecimal("10.0"), new BigDecimal("3.50"),
                    new BigDecimal("35.00"), false, false);

            ResponseEntity<FillupResponse> response = restTemplate.postForEntity(
                    "/api/fillups", request, FillupResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            // MPG = (10300 - 10000) / (5.0 + 10.0) = 300 / 15 = 20 MPG
            assertThat(response.getBody().mpg()).isEqualByComparingTo(new BigDecimal("20.00"));
        }
    }
}
