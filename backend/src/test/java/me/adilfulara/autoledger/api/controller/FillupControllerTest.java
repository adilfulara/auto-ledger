package me.adilfulara.autoledger.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.adilfulara.autoledger.api.dto.CreateFillupRequest;
import me.adilfulara.autoledger.api.dto.UpdateFillupRequest;
import me.adilfulara.autoledger.api.exception.GlobalExceptionHandler;
import me.adilfulara.autoledger.api.exception.ResourceNotFoundException;
import me.adilfulara.autoledger.domain.model.Fillup;
import me.adilfulara.autoledger.domain.repository.CarRepository;
import me.adilfulara.autoledger.domain.repository.FillupRepository;
import me.adilfulara.autoledger.service.FillupService;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FillupController")
class FillupControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private FillupRepository fillupRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private FillupService fillupService;

    @InjectMocks
    private FillupController fillupController;

    private static final UUID CAR_ID = UUID.randomUUID();
    private static final UUID FILLUP_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fillupController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private Fillup createTestFillup() {
        Fillup fillup = new Fillup(CAR_ID, Instant.now(), 10000L,
                new BigDecimal("10.0"), new BigDecimal("3.50"),
                new BigDecimal("35.00"), false, false);
        fillup.setId(FILLUP_ID);
        return fillup;
    }

    @Nested
    @DisplayName("GET /api/fillups/{id}")
    class GetFillup {

        @Test
        @DisplayName("returns fillup with MPG when found")
        void returnsFillupWithMPG() throws Exception {
            Fillup fillup = createTestFillup();
            when(fillupRepository.findById(FILLUP_ID)).thenReturn(Optional.of(fillup));
            when(fillupService.calculateMPG(fillup)).thenReturn(Optional.of(new BigDecimal("30.00")));

            mockMvc.perform(get("/api/fillups/{id}", FILLUP_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(FILLUP_ID.toString()))
                    .andExpect(jsonPath("$.carId").value(CAR_ID.toString()))
                    .andExpect(jsonPath("$.mpg").value(30.00));
        }

        @Test
        @DisplayName("returns 404 when not found")
        void returns404WhenNotFound() throws Exception {
            when(fillupRepository.findById(FILLUP_ID)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/fillups/{id}", FILLUP_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/fillups")
    class CreateFillup {

        @Test
        @DisplayName("creates fillup with valid request")
        void createsFillup() throws Exception {
            CreateFillupRequest request = new CreateFillupRequest(
                    CAR_ID, Instant.now(), 10000L,
                    new BigDecimal("10.0"), new BigDecimal("3.50"),
                    new BigDecimal("35.00"), false, false);
            Fillup fillup = createTestFillup();

            when(carRepository.existsById(CAR_ID)).thenReturn(true);
            when(fillupRepository.findMostRecentByCarId(CAR_ID)).thenReturn(Optional.empty());
            when(fillupRepository.save(any(Fillup.class))).thenReturn(fillup);
            when(fillupService.calculateMPG(fillup)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/fillups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.carId").value(CAR_ID.toString()));
        }

        @Test
        @DisplayName("returns 404 when car not found")
        void returns404WhenCarNotFound() throws Exception {
            CreateFillupRequest request = new CreateFillupRequest(
                    CAR_ID, Instant.now(), 10000L,
                    new BigDecimal("10.0"), new BigDecimal("3.50"),
                    new BigDecimal("35.00"), false, false);

            when(carRepository.existsById(CAR_ID)).thenReturn(false);

            mockMvc.perform(post("/api/fillups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 400 when odometer is not greater than previous")
        void returns400WhenOdometerInvalid() throws Exception {
            Fillup existingFillup = createTestFillup();
            existingFillup.setOdometer(15000L);

            CreateFillupRequest request = new CreateFillupRequest(
                    CAR_ID, Instant.now(), 10000L,
                    new BigDecimal("10.0"), new BigDecimal("3.50"),
                    new BigDecimal("35.00"), false, false);

            when(carRepository.existsById(CAR_ID)).thenReturn(true);
            when(fillupRepository.findMostRecentByCarId(CAR_ID)).thenReturn(Optional.of(existingFillup));

            mockMvc.perform(post("/api/fillups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("returns 400 for invalid request")
        void returns400ForInvalidRequest() throws Exception {
            CreateFillupRequest request = new CreateFillupRequest(
                    null, null, null, null, null, null, null, null);

            mockMvc.perform(post("/api/fillups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/fillups/{id}")
    class UpdateFillup {

        @Test
        @DisplayName("updates fillup with partial request")
        void updatesFillupPartial() throws Exception {
            UpdateFillupRequest request = new UpdateFillupRequest(
                    null, 10500L, null, null, null, null, null);
            Fillup fillup = createTestFillup();

            when(fillupRepository.findById(FILLUP_ID)).thenReturn(Optional.of(fillup));
            when(fillupRepository.save(any(Fillup.class))).thenReturn(fillup);
            when(fillupService.calculateMPG(fillup)).thenReturn(Optional.of(new BigDecimal("30.00")));

            mockMvc.perform(put("/api/fillups/{id}", FILLUP_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("updates all fillup fields")
        void updatesAllFields() throws Exception {
            UpdateFillupRequest request = new UpdateFillupRequest(
                    Instant.now(), 10500L, new BigDecimal("12.5"),
                    new BigDecimal("3.75"), new BigDecimal("46.88"), true, true);
            Fillup fillup = createTestFillup();

            when(fillupRepository.findById(FILLUP_ID)).thenReturn(Optional.of(fillup));
            when(fillupRepository.save(any(Fillup.class))).thenReturn(fillup);
            when(fillupService.calculateMPG(fillup)).thenReturn(Optional.of(new BigDecimal("28.00")));

            mockMvc.perform(put("/api/fillups/{id}", FILLUP_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 404 when fillup not found")
        void returns404WhenNotFound() throws Exception {
            UpdateFillupRequest request = new UpdateFillupRequest(
                    null, 10500L, null, null, null, null, null);

            when(fillupRepository.findById(FILLUP_ID)).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/fillups/{id}", FILLUP_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/fillups/{id}")
    class DeleteFillup {

        @Test
        @DisplayName("deletes fillup successfully")
        void deletesFillup() throws Exception {
            when(fillupRepository.existsById(FILLUP_ID)).thenReturn(true);

            mockMvc.perform(delete("/api/fillups/{id}", FILLUP_ID))
                    .andExpect(status().isNoContent());

            verify(fillupRepository).deleteById(FILLUP_ID);
        }

        @Test
        @DisplayName("returns 404 when not found")
        void returns404WhenNotFound() throws Exception {
            when(fillupRepository.existsById(FILLUP_ID)).thenReturn(false);

            mockMvc.perform(delete("/api/fillups/{id}", FILLUP_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/cars/{carId}/fillups")
    class GetFillupsByCarId {

        @Test
        @DisplayName("returns fillups for car")
        void returnsFillups() throws Exception {
            Fillup fillup = createTestFillup();
            when(carRepository.existsById(CAR_ID)).thenReturn(true);
            when(fillupRepository.findByCarIdOrderByDateDesc(CAR_ID)).thenReturn(List.of(fillup));
            when(fillupService.calculateMPG(fillup)).thenReturn(Optional.of(new BigDecimal("30.00")));

            mockMvc.perform(get("/api/cars/{carId}/fillups", CAR_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(FILLUP_ID.toString()))
                    .andExpect(jsonPath("$[0].mpg").value(30.00));
        }

        @Test
        @DisplayName("returns 404 when car not found")
        void returns404WhenCarNotFound() throws Exception {
            when(carRepository.existsById(CAR_ID)).thenReturn(false);

            mockMvc.perform(get("/api/cars/{carId}/fillups", CAR_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/cars/{carId}/fillups/recent")
    class GetRecentFillups {

        @Test
        @DisplayName("returns recent fillups with default limit")
        void returnsRecentFillups() throws Exception {
            Fillup fillup = createTestFillup();
            when(carRepository.existsById(CAR_ID)).thenReturn(true);
            when(fillupRepository.findRecentByCarId(CAR_ID, 50)).thenReturn(List.of(fillup));
            when(fillupService.calculateMPG(fillup)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/cars/{carId}/fillups/recent", CAR_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(FILLUP_ID.toString()));
        }

        @Test
        @DisplayName("returns recent fillups with custom limit")
        void returnsRecentFillupsWithLimit() throws Exception {
            when(carRepository.existsById(CAR_ID)).thenReturn(true);
            when(fillupRepository.findRecentByCarId(CAR_ID, 10)).thenReturn(List.of());

            mockMvc.perform(get("/api/cars/{carId}/fillups/recent", CAR_ID)
                            .param("limit", "10"))
                    .andExpect(status().isOk());

            verify(fillupRepository).findRecentByCarId(CAR_ID, 10);
        }
    }
}
