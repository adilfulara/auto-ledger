package me.adilfulara.autoledger.domain.repository;

import me.adilfulara.autoledger.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for database schema using Testcontainers.
 * Tests run against real PostgreSQL to verify Flyway migrations and entity mappings.
 */
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseSchemaIT extends me.adilfulara.autoledger.BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private FillupRepository fillupRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        fillupRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User("user_test123", "test@example.com");
        testUser = userRepository.save(testUser);
    }

    @Test
    void testUserCRUDOperations() {
        // Verify user was saved
        assertThat(testUser.getId()).isNotNull();
        assertThat(testUser.getAuthProviderId()).isEqualTo("user_test123");
        assertThat(testUser.getEmail()).isEqualTo("test@example.com");
        assertThat(testUser.getCreatedAt()).isNotNull();

        // Find by internal ID
        Optional<User> found = userRepository.findById(testUser.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");

        // Find by auth provider ID
        Optional<User> foundByAuthId = userRepository.findByAuthProviderId("user_test123");
        assertThat(foundByAuthId).isPresent();
        assertThat(foundByAuthId.get().getId()).isEqualTo(testUser.getId());

        // Find by email
        Optional<User> foundByEmail = userRepository.findByEmail("test@example.com");
        assertThat(foundByEmail).isPresent();

        // Check exists
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.existsByAuthProviderId("user_test123")).isTrue();
    }

    @Test
    void testCarCRUDOperations() {
        // Create car
        Car car = new Car(
                testUser.getId(),
                "Tesla",
                "Model 3",
                2023,
                "5YJ3E1EA1KF000001",
                "My Tesla",
                FuelUnit.GALLONS,
                DistanceUnit.MILES
        );
        car = carRepository.save(car);

        // Verify car
        assertThat(car.getId()).isNotNull();
        assertThat(car.getMake()).isEqualTo("Tesla");
        assertThat(car.getFuelUnit()).isEqualTo(FuelUnit.GALLONS);

        // Find by user
        List<Car> userCars = carRepository.findByUserId(testUser.getId());
        assertThat(userCars).hasSize(1);

        // Find by VIN
        Optional<Car> foundByVin = carRepository.findByVin("5YJ3E1EA1KF000001");
        assertThat(foundByVin).isPresent();
    }

    @Test
    void testFillupCRUDOperations() {
        // Create car
        Car car = new Car(
                testUser.getId(),
                "Honda",
                "Civic",
                2020,
                null,
                "My Civic",
                FuelUnit.GALLONS,
                DistanceUnit.MILES
        );
        car = carRepository.save(car);

        // Create fillup
        Fillup fillup = new Fillup(
                car.getId(),
                Instant.now(),
                50000L,
                new BigDecimal("12.500"),
                new BigDecimal("3.999"),
                new BigDecimal("49.99"),
                false,
                false
        );
        fillup = fillupRepository.save(fillup);

        // Verify fillup
        assertThat(fillup.getId()).isNotNull();
        assertThat(fillup.getFuelVolume()).isEqualByComparingTo(new BigDecimal("12.500"));
        assertThat(fillup.getIsPartial()).isFalse();

        // Find by car
        List<Fillup> carFillups = fillupRepository.findByCarIdOrderByDateDesc(car.getId());
        assertThat(carFillups).hasSize(1);

        // Test null handling for isPartial and isMissed (branch coverage)
        Fillup fillupWithNulls = new Fillup(
                car.getId(),
                Instant.now(),
                51000L,
                new BigDecimal("10.000"),
                new BigDecimal("4.00"),
                new BigDecimal("40.00"),
                null,  // Should default to false
                null   // Should default to false
        );
        fillupWithNulls = fillupRepository.save(fillupWithNulls);
        assertThat(fillupWithNulls.getIsPartial()).isFalse();
        assertThat(fillupWithNulls.getIsMissed()).isFalse();
    }

    @Test
    void testCascadeDelete() {
        // Create car with fillup
        Car car = new Car(
                testUser.getId(),
                "Ford",
                "F-150",
                2022,
                null,
                "My Truck",
                FuelUnit.GALLONS,
                DistanceUnit.MILES
        );
        car = carRepository.save(car);

        Fillup fillup = new Fillup(
                car.getId(),
                Instant.now(),
                10000L,
                new BigDecimal("20.000"),
                new BigDecimal("4.00"),
                new BigDecimal("80.00"),
                false,
                false
        );
        fillupRepository.save(fillup);

        // Delete car
        carRepository.deleteById(car.getId());

        // Verify fillup was cascade deleted
        assertThat(fillupRepository.countByCarId(car.getId())).isEqualTo(0);
    }
}
