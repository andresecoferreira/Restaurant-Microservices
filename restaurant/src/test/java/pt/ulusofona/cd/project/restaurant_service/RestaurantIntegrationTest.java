package pt.ulusofona.cd.project.restaurant_service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pt.ulusofona.cd.project.restaurant_service.dto.BookingRequestDTO;
import pt.ulusofona.cd.project.restaurant_service.dto.RestaurantDTO;
import pt.ulusofona.cd.project.restaurant_service.dto.TimeSlotDTO;
import pt.ulusofona.cd.project.restaurant_service.service.RestaurantService;
import pt.ulusofona.cd.project.restaurant_service.service.TimeSlotService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class RestaurantIntegrationTest {

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
    void testBookingWorkflow() throws Exception {
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

        // 2. Create an availability slot
        LocalDate slotDate = LocalDate.now();
        java.time.LocalTime startTime = java.time.LocalTime.of(19, 0);
        java.time.LocalTime endTime = java.time.LocalTime.of(22, 0);

        TimeSlotDTO timeSlotDTO = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(slotDate)
                .startTime(startTime)
                .endTime(endTime)
                .capacity(5)
                .seatsAvailable(5)
                .build();

        TimeSlotDTO createdSlot = timeSlotService.createTimeSlot(restaurantId, timeSlotDTO);
        Long slotId = createdSlot.getId();

        // 3. Get availability - should show the slot
        mockMvc.perform(get("/api/restaurants/{id}/availability", restaurantId)
                .param("date", slotDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].id", equalTo(slotId.intValue())))
                .andExpect(jsonPath("$[0].seatsAvailable", equalTo(5)));

        // 4. Book 2 seats
        BookingRequestDTO bookingRequest = BookingRequestDTO.builder()
                .seatCount(2)
                .build();

        mockMvc.perform(post("/api/restaurants/{restaurantId}/slots/{slotId}/book", restaurantId, slotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatsAvailable", equalTo(3)));

        // 5. Verify the booking in database
        TimeSlotDTO updatedSlot = timeSlotService.bookSeats(restaurantId, slotId, 1);
        assert updatedSlot.getSeatsAvailable() == 2;

        // 6. Try to book more than available - should fail
        BookingRequestDTO overbookRequest = BookingRequestDTO.builder()
                .seatCount(10)
                .build();

        mockMvc.perform(post("/api/restaurants/{restaurantId}/slots/{slotId}/book", restaurantId, slotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(overbookRequest)))
                .andExpect(status().isConflict());

        // 7. Release 1 seat
        mockMvc.perform(post("/api/restaurants/slots/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\": " + slotId + ", \"seatCount\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatsAvailable", equalTo(3)));
    }

    @Test
    void testAvailabilityForInactiveRestaurant() throws Exception {
        // 1. Create a restaurant (initially active)
        RestaurantDTO restaurantDTO = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Inactive Restaurant")
                        .city("Test City")
                        .country("Test Country")
                        .phone("123-456-7890")
                        .email("inactive@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId = restaurantDTO.getId();

        // 2. Create an availability slot
        LocalDate slotDate = LocalDate.now();
        TimeSlotDTO timeSlotDTO = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(slotDate)
                .startTime(java.time.LocalTime.of(20, 0))
                .endTime(java.time.LocalTime.of(22, 0))
                .capacity(10)
                .seatsAvailable(10)
                .build();
        timeSlotService.createTimeSlot(restaurantId, timeSlotDTO);

        // 3. Deactivate the restaurant
        restaurantService.updateRestaurant(restaurantId,
                RestaurantDTO.builder()
                        .active(false)
                        .build()
        );

        // 4. Check availability - should return empty
        mockMvc.perform(get("/api/restaurants/{id}/availability", restaurantId)
                .param("date", slotDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testNotFoundError() throws Exception {
        mockMvc.perform(get("/api/restaurants/{id}/availability", 999999)
                .param("date", LocalDate.now().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)))
                .andExpect(jsonPath("$.error", equalTo("NOT_FOUND")));
    }

    @Test
    void testGetRestaurantSlots() throws Exception {
        // 1. Create a restaurant
        RestaurantDTO restaurantDTO = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Slot Test Restaurant")
                        .city("Test City")
                        .country("Test Country")
                        .phone("123-456-7890")
                        .email("slots@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId = restaurantDTO.getId();

        // 2. Create multiple time slots on different dates
        LocalDate date1 = LocalDate.now();
        LocalDate date2 = LocalDate.now().plusDays(1);

        TimeSlotDTO slot1 = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(date1)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(14, 0))
                .capacity(10)
                .seatsAvailable(10)
                .build();
        TimeSlotDTO createdSlot1 = timeSlotService.createTimeSlot(restaurantId, slot1);

        TimeSlotDTO slot2 = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(date1)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(22, 0))
                .capacity(15)
                .seatsAvailable(15)
                .build();
        TimeSlotDTO createdSlot2 = timeSlotService.createTimeSlot(restaurantId, slot2);

        TimeSlotDTO slot3 = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(date2)
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(23, 0))
                .capacity(12)
                .seatsAvailable(12)
                .build();
        TimeSlotDTO createdSlot3 = timeSlotService.createTimeSlot(restaurantId, slot3);

        // 3. GET /api/restaurants/{id}/slots - should return all slots
        mockMvc.perform(get("/api/restaurants/{id}/slots", restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].restaurantId", everyItem(equalTo(restaurantId.intValue()))))
                .andExpect(jsonPath("$[0].capacity", equalTo(10)))
                .andExpect(jsonPath("$[1].capacity", equalTo(15)))
                .andExpect(jsonPath("$[2].capacity", equalTo(12)));
    }

    @Test
    void testGetRestaurantSlotsEmpty() throws Exception {
        // 1. Create a restaurant with no slots
        RestaurantDTO restaurantDTO = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Empty Slots Restaurant")
                        .city("Test City")
                        .country("Test Country")
                        .phone("123-456-7890")
                        .email("empty-slots@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId = restaurantDTO.getId();

        // 2. GET /api/restaurants/{id}/slots - should return empty list
        mockMvc.perform(get("/api/restaurants/{id}/slots", restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetRestaurantSlotsNotFound() throws Exception {
        // GET /api/restaurants/{id}/slots with non-existent restaurant - should return 404
        mockMvc.perform(get("/api/restaurants/{id}/slots", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)))
                .andExpect(jsonPath("$.error", equalTo("NOT_FOUND")));
    }

    @Test
    void testGetRestaurantSlotsIsolation() throws Exception {
        // 1. Create two restaurants
        RestaurantDTO restaurant1 = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Restaurant 1")
                        .city("City 1")
                        .country("Country 1")
                        .phone("123-456-7890")
                        .email("rest1@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId1 = restaurant1.getId();

        RestaurantDTO restaurant2 = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Restaurant 2")
                        .city("City 2")
                        .country("Country 2")
                        .phone("123-456-7890")
                        .email("rest2@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId2 = restaurant2.getId();

        // 2. Create slots for both restaurants
        LocalDate today = LocalDate.now();
        TimeSlotDTO slot1 = TimeSlotDTO.builder()
                .restaurantId(restaurantId1)
                .date(today)
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(22, 0))
                .capacity(10)
                .seatsAvailable(10)
                .build();
        timeSlotService.createTimeSlot(restaurantId1, slot1);

        TimeSlotDTO slot2 = TimeSlotDTO.builder()
                .restaurantId(restaurantId2)
                .date(today)
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(22, 0))
                .capacity(20)
                .seatsAvailable(20)
                .build();
        timeSlotService.createTimeSlot(restaurantId2, slot2);

        // 3. GET /api/restaurants/{id}/slots for each restaurant
        mockMvc.perform(get("/api/restaurants/{id}/slots", restaurantId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].capacity", equalTo(10)));

        mockMvc.perform(get("/api/restaurants/{id}/slots", restaurantId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].capacity", equalTo(20)));
    }

    @Test
    void testGetSlotByIdIntegration() throws Exception {
        // 1. Create a restaurant
        RestaurantDTO restaurantDTO = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Slot By ID Restaurant")
                        .city("Test City")
                        .country("Test Country")
                        .phone("123-456-7890")
                        .email("slotbyid@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId = restaurantDTO.getId();

        // 2. Create a time slot
        LocalDate slotDate = LocalDate.now();
        TimeSlotDTO timeSlotDTO = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(slotDate)
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(23, 0))
                .capacity(8)
                .seatsAvailable(8)
                .build();
        TimeSlotDTO createdSlot = timeSlotService.createTimeSlot(restaurantId, timeSlotDTO);
        Long slotId = createdSlot.getId();

        // 3. GET /api/slots/{id} - should return slot details
        mockMvc.perform(get("/api/slots/{id}", slotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(slotId.intValue())))
                .andExpect(jsonPath("$.restaurantId", equalTo(restaurantId.intValue())))
                .andExpect(jsonPath("$.date", equalTo(slotDate.toString())))
                .andExpect(jsonPath("$.capacity", equalTo(8)))
                .andExpect(jsonPath("$.seatsAvailable", equalTo(8)));
    }

    @Test
    void testGetSlotByIdNotFoundIntegration() throws Exception {
        // GET /api/slots/{id} with non-existent slot - should return 404
        mockMvc.perform(get("/api/slots/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)))
                .andExpect(jsonPath("$.error", equalTo("NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("Slot not found")));
    }

    @Test
    void testGetSlotByIdWithBookingIntegration() throws Exception {
        // 1. Create restaurant and slot
        RestaurantDTO restaurantDTO = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Booking Integration Restaurant")
                        .city("Test City")
                        .country("Test Country")
                        .phone("123-456-7890")
                        .email("booking-int@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId = restaurantDTO.getId();

        LocalDate slotDate = LocalDate.now();
        TimeSlotDTO timeSlotDTO = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(slotDate)
                .startTime(LocalTime.of(19, 30))
                .endTime(LocalTime.of(22, 30))
                .capacity(6)
                .seatsAvailable(6)
                .build();
        TimeSlotDTO createdSlot = timeSlotService.createTimeSlot(restaurantId, timeSlotDTO);
        Long slotId = createdSlot.getId();

        // 2. Book seats through the API
        BookingRequestDTO bookingRequest = BookingRequestDTO.builder()
                .seatCount(2)
                .build();

        mockMvc.perform(post("/api/restaurants/{restaurantId}/slots/{slotId}/book", restaurantId, slotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatsAvailable", equalTo(4)));

        // 3. GET /api/slots/{id} - should show updated availability
        mockMvc.perform(get("/api/slots/{id}", slotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatsAvailable", equalTo(4)))
                .andExpect(jsonPath("$.capacity", equalTo(6)));
    }
}
