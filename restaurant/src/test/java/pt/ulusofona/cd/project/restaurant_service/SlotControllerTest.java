package pt.ulusofona.cd.project.restaurant_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pt.ulusofona.cd.project.restaurant_service.dto.RestaurantDTO;
import pt.ulusofona.cd.project.restaurant_service.dto.TimeSlotDTO;
import pt.ulusofona.cd.project.restaurant_service.service.RestaurantService;
import pt.ulusofona.cd.project.restaurant_service.service.TimeSlotService;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class SlotControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("restaurant_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetSlotByIdSuccess() throws Exception {
        // 1. Create a restaurant
        RestaurantDTO restaurantDTO = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Test Restaurant")
                        .city("Test City")
                        .country("Test Country")
                        .phone("123-456-7890")
                        .email("test@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId = restaurantDTO.getId();

        // 2. Create a time slot
        LocalDate slotDate = LocalDate.now();
        TimeSlotDTO timeSlotDTO = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(slotDate)
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(22, 0))
                .capacity(10)
                .seatsAvailable(10)
                .build();

        TimeSlotDTO createdSlot = timeSlotService.createTimeSlot(restaurantId, timeSlotDTO);
        Long slotId = createdSlot.getId();

        // 3. GET /api/slots/{id} - should return 200 with slot details
        mockMvc.perform(get("/api/slots/{id}", slotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(slotId.intValue())))
                .andExpect(jsonPath("$.restaurantId", equalTo(restaurantId.intValue())))
                .andExpect(jsonPath("$.date", equalTo(slotDate.toString())))
                .andExpect(jsonPath("$.startTime", equalTo("19:00:00")))
                .andExpect(jsonPath("$.endTime", equalTo("22:00:00")))
                .andExpect(jsonPath("$.capacity", equalTo(10)))
                .andExpect(jsonPath("$.seatsAvailable", equalTo(10)));
    }

    @Test
    void testGetSlotByIdNotFound() throws Exception {
        // GET /api/slots/{id} with non-existent ID - should return 404
        mockMvc.perform(get("/api/slots/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)))
                .andExpect(jsonPath("$.error", equalTo("NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("Slot not found")));
    }

    @Test
    void testGetSlotByIdAfterBooking() throws Exception {
        // 1. Create restaurant and slot
        RestaurantDTO restaurantDTO = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Test Restaurant 2")
                        .city("Test City")
                        .country("Test Country")
                        .phone("123-456-7890")
                        .email("test2@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId = restaurantDTO.getId();

        LocalDate slotDate = LocalDate.now();
        TimeSlotDTO timeSlotDTO = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(slotDate)
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(23, 0))
                .capacity(5)
                .seatsAvailable(5)
                .build();

        TimeSlotDTO createdSlot = timeSlotService.createTimeSlot(restaurantId, timeSlotDTO);
        Long slotId = createdSlot.getId();

        // 2. Book some seats
        timeSlotService.bookSeats(restaurantId, slotId, 2);

        // 3. GET /api/slots/{id} - should reflect reduced seats
        mockMvc.perform(get("/api/slots/{id}", slotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatsAvailable", equalTo(3)))
                .andExpect(jsonPath("$.capacity", equalTo(5)));
    }

    @Test
    void testGetSlotByIdWithReleaseSeats() throws Exception {
        // 1. Create restaurant and slot
        RestaurantDTO restaurantDTO = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Test Restaurant 3")
                        .city("Test City")
                        .country("Test Country")
                        .phone("123-456-7890")
                        .email("test3@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId = restaurantDTO.getId();

        LocalDate slotDate = LocalDate.now();
        TimeSlotDTO timeSlotDTO = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(slotDate)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 0))
                .capacity(8)
                .seatsAvailable(8)
                .build();

        TimeSlotDTO createdSlot = timeSlotService.createTimeSlot(restaurantId, timeSlotDTO);
        Long slotId = createdSlot.getId();

        // 2. Book 3 seats
        timeSlotService.bookSeats(restaurantId, slotId, 3);

        // 3. Release 1 seat
        timeSlotService.releaseSeats(slotId, 1);

        // 4. GET /api/slots/{id} - should show 6 available seats
        mockMvc.perform(get("/api/slots/{id}", slotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatsAvailable", equalTo(6)))
                .andExpect(jsonPath("$.capacity", equalTo(8)));
    }
}
