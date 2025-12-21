package me.adilfulara.autoledger.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.adilfulara.autoledger.api.dto.CarStatsResponse;
import me.adilfulara.autoledger.api.dto.CreateCarRequest;
import me.adilfulara.autoledger.api.dto.UpdateCarRequest;
import me.adilfulara.autoledger.api.exception.GlobalExceptionHandler;
import me.adilfulara.autoledger.api.exception.ResourceNotFoundException;
import me.adilfulara.autoledger.auth.AuthenticatedUser;
import me.adilfulara.autoledger.auth.CurrentUserResolver;
import me.adilfulara.autoledger.auth.JwtAuthFilter;
import me.adilfulara.autoledger.domain.model.Car;
import me.adilfulara.autoledger.domain.model.DistanceUnit;
import me.adilfulara.autoledger.domain.model.FuelUnit;
import me.adilfulara.autoledger.service.CarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarController")
class CarControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private CarService carService;

    @InjectMocks
    private CarController carController;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CAR_ID = UUID.randomUUID();
    private static final AuthenticatedUser TEST_USER = new AuthenticatedUser(
        USER_ID, "test_user", "test@example.com"
    );

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(carController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentUserResolver())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private Car createTestCar() {
        Car car = new Car(USER_ID, "Toyota", "Camry", 2022, null, "My Car",
                FuelUnit.GALLONS, DistanceUnit.MILES);
        car.setId(CAR_ID);
        return car;
    }

    @Nested
    @DisplayName("GET /api/cars")
    class ListCars {

        @Test
        @DisplayName("returns list of cars for user")
        void returnsListOfCars() throws Exception {
            Car car = createTestCar();
            when(carService.getCarsByUserId(USER_ID)).thenReturn(List.of(car));

            mockMvc.perform(get("/api/cars")
                            .requestAttr(JwtAuthFilter.AUTHENTICATED_USER_ATTRIBUTE, TEST_USER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(CAR_ID.toString()))
                    .andExpect(jsonPath("$[0].make").value("Toyota"))
                    .andExpect(jsonPath("$[0].model").value("Camry"));
        }

        @Test
        @DisplayName("returns empty list when no cars")
        void returnsEmptyList() throws Exception {
            when(carService.getCarsByUserId(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/api/cars")
                            .requestAttr(JwtAuthFilter.AUTHENTICATED_USER_ATTRIBUTE, TEST_USER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/cars/{id}")
    class GetCar {

        @Test
        @DisplayName("returns car when found")
        void returnsCar() throws Exception {
            Car car = createTestCar();
            when(carService.getCarById(CAR_ID)).thenReturn(car);

            mockMvc.perform(get("/api/cars/{id}", CAR_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(CAR_ID.toString()))
                    .andExpect(jsonPath("$.make").value("Toyota"));
        }

        @Test
        @DisplayName("returns 404 when not found")
        void returns404WhenNotFound() throws Exception {
            when(carService.getCarById(CAR_ID))
                    .thenThrow(new ResourceNotFoundException("Car", CAR_ID));

            mockMvc.perform(get("/api/cars/{id}", CAR_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }
    }

    @Nested
    @DisplayName("POST /api/cars")
    class CreateCar {

        @Test
        @DisplayName("creates car with valid request")
        void createsCar() throws Exception {
            CreateCarRequest request = new CreateCarRequest(
                    "Honda", "Accord", 2023, null, "Family Car",
                    FuelUnit.GALLONS, DistanceUnit.MILES);
            Car car = new Car(USER_ID, "Honda", "Accord", 2023, null, "Family Car",
                    FuelUnit.GALLONS, DistanceUnit.MILES);
            car.setId(CAR_ID);

            when(carService.createCar(eq(USER_ID), any(CreateCarRequest.class))).thenReturn(car);

            mockMvc.perform(post("/api/cars")
                            .requestAttr(JwtAuthFilter.AUTHENTICATED_USER_ATTRIBUTE, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.make").value("Honda"))
                    .andExpect(jsonPath("$.model").value("Accord"));
        }

        @Test
        @DisplayName("returns 400 for invalid request")
        void returns400ForInvalidRequest() throws Exception {
            CreateCarRequest request = new CreateCarRequest(
                    "", null, null, null, null, null, null);

            mockMvc.perform(post("/api/cars")
                            .requestAttr(JwtAuthFilter.AUTHENTICATED_USER_ATTRIBUTE, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/cars/{id}")
    class UpdateCar {

        @Test
        @DisplayName("updates car with valid request")
        void updatesCar() throws Exception {
            UpdateCarRequest request = new UpdateCarRequest("Honda", "Civic", 2024, null, "Updated");
            Car updatedCar = new Car(USER_ID, "Honda", "Civic", 2024, null, "Updated",
                    FuelUnit.GALLONS, DistanceUnit.MILES);
            updatedCar.setId(CAR_ID);

            when(carService.updateCar(eq(CAR_ID), any(UpdateCarRequest.class))).thenReturn(updatedCar);

            mockMvc.perform(put("/api/cars/{id}", CAR_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.make").value("Honda"))
                    .andExpect(jsonPath("$.model").value("Civic"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/cars/{id}")
    class DeleteCar {

        @Test
        @DisplayName("deletes car successfully")
        void deletesCar() throws Exception {
            doNothing().when(carService).deleteCar(CAR_ID);

            mockMvc.perform(delete("/api/cars/{id}", CAR_ID))
                    .andExpect(status().isNoContent());

            verify(carService).deleteCar(CAR_ID);
        }

        @Test
        @DisplayName("returns 404 when car not found")
        void returns404WhenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Car", CAR_ID)).when(carService).deleteCar(CAR_ID);

            mockMvc.perform(delete("/api/cars/{id}", CAR_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/cars/{id}/stats")
    class GetCarStats {

        @Test
        @DisplayName("returns car statistics")
        void returnsStats() throws Exception {
            CarStatsResponse stats = new CarStatsResponse(
                    CAR_ID, "My Car", 10L, 3000L,
                    new BigDecimal("100.00"), new BigDecimal("350.00"),
                    new BigDecimal("30.00"), new BigDecimal("35.00"),
                    new BigDecimal("25.00"), new BigDecimal("3.50"));

            when(carService.getCarStats(CAR_ID)).thenReturn(stats);

            mockMvc.perform(get("/api/cars/{id}/stats", CAR_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.carId").value(CAR_ID.toString()))
                    .andExpect(jsonPath("$.totalFillups").value(10))
                    .andExpect(jsonPath("$.averageMpg").value(30.00));
        }
    }
}
