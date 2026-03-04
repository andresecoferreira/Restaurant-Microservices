package pt.ulusofona.cd.project.restaurant_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pt.ulusofona.cd.project.restaurant_service.dto.RestaurantDTO;
import pt.ulusofona.cd.project.restaurant_service.dto.TimeSlotDTO;
import pt.ulusofona.cd.project.restaurant_service.service.RestaurantService;
import pt.ulusofona.cd.project.restaurant_service.service.TimeSlotService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class RestaurantServiceTest {

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
    private RestaurantService restaurantService;

    @Autowired
    private TimeSlotService timeSlotService;

    @Test
    void testGetSlotByIdSuccess() {
        // 1. Create restaurant
        RestaurantDTO restaurantDTO = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Pizza Place")
                        .city("Rome")
                        .country("Italy")
                        .phone("123-456-7890")
                        .email("pizza@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId = restaurantDTO.getId();

        // 2. Create time slot
        TimeSlotDTO timeSlotDTO = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(LocalDate.now())
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(22, 0))
                .capacity(10)
                .seatsAvailable(10)
                .build();

        TimeSlotDTO createdSlot = timeSlotService.createTimeSlot(restaurantId, timeSlotDTO);
        Long slotId = createdSlot.getId();

        // 3. Get slot by ID
        TimeSlotDTO retrievedSlot = restaurantService.getSlotById(slotId);

        // 4. Verify
        assertNotNull(retrievedSlot);
        assertEquals(slotId, retrievedSlot.getId());
        assertEquals(restaurantId, retrievedSlot.getRestaurantId());
        assertEquals(10, retrievedSlot.getCapacity());
        assertEquals(10, retrievedSlot.getSeatsAvailable());
    }

    @Test
    void testGetSlotByIdNotFound() {
        // Test getting non-existent slot
        assertThrows(NoSuchElementException.class, () -> {
            restaurantService.getSlotById(999999L);
        });
    }

    @Test
    void testGetSlotsByRestaurantIdSuccess() {
        // 1. Create restaurant
        RestaurantDTO restaurantDTO = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Burger House")
                        .city("New York")
                        .country("USA")
                        .phone("123-456-7890")
                        .email("burger@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId = restaurantDTO.getId();

        // 2. Create multiple time slots
        LocalDate today = LocalDate.now();

        TimeSlotDTO slot1 = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(today)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(14, 0))
                .capacity(15)
                .seatsAvailable(15)
                .build();
        timeSlotService.createTimeSlot(restaurantId, slot1);

        TimeSlotDTO slot2 = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(today)
                .startTime(LocalTime.of(17, 0))
                .endTime(LocalTime.of(21, 0))
                .capacity(20)
                .seatsAvailable(20)
                .build();
        timeSlotService.createTimeSlot(restaurantId, slot2);

        TimeSlotDTO slot3 = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(today.plusDays(1))
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(14, 0))
                .capacity(15)
                .seatsAvailable(15)
                .build();
        timeSlotService.createTimeSlot(restaurantId, slot3);

        // 3. Get all slots for restaurant
        List<TimeSlotDTO> slots = restaurantService.getSlotsByRestaurantId(restaurantId);

        // 4. Verify
        assertNotNull(slots);
        assertEquals(3, slots.size());
        assertTrue(slots.stream().allMatch(s -> s.getRestaurantId().equals(restaurantId)));
    }

    @Test
    void testGetSlotsByRestaurantIdEmpty() {
        // 1. Create restaurant with no slots
        RestaurantDTO restaurantDTO = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Empty Restaurant")
                        .city("Paris")
                        .country("France")
                        .phone("123-456-7890")
                        .email("empty@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId = restaurantDTO.getId();

        // 2. Get slots for restaurant with no slots
        List<TimeSlotDTO> slots = restaurantService.getSlotsByRestaurantId(restaurantId);

        // 3. Verify empty list
        assertNotNull(slots);
        assertEquals(0, slots.size());
    }

    @Test
    void testGetSlotsByRestaurantIdRestaurantNotFound() {
        // Test getting slots for non-existent restaurant
        assertThrows(NoSuchElementException.class, () -> {
            restaurantService.getSlotsByRestaurantId(999999L);
        });
    }

    @Test
    void testGetSlotsByRestaurantIdOrderedByDate() {
        // 1. Create restaurant
        RestaurantDTO restaurantDTO = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Ordered Restaurant")
                        .city("Barcelona")
                        .country("Spain")
                        .phone("123-456-7890")
                        .email("ordered@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId = restaurantDTO.getId();

        // 2. Create slots in non-chronological order
        LocalDate date1 = LocalDate.now();
        LocalDate date2 = LocalDate.now().plusDays(2);
        LocalDate date3 = LocalDate.now().plusDays(1);

        TimeSlotDTO slot1 = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(date1)
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(22, 0))
                .capacity(10)
                .seatsAvailable(10)
                .build();
        timeSlotService.createTimeSlot(restaurantId, slot1);

        TimeSlotDTO slot2 = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(date2)
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(22, 0))
                .capacity(10)
                .seatsAvailable(10)
                .build();
        timeSlotService.createTimeSlot(restaurantId, slot2);

        TimeSlotDTO slot3 = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(date3)
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(22, 0))
                .capacity(10)
                .seatsAvailable(10)
                .build();
        timeSlotService.createTimeSlot(restaurantId, slot3);

        // 3. Get slots - should be ordered by date
        List<TimeSlotDTO> slots = restaurantService.getSlotsByRestaurantId(restaurantId);

        // 4. Verify order
        assertEquals(3, slots.size());
        assertEquals(date1, slots.get(0).getDate());
        assertEquals(date3, slots.get(1).getDate());
        assertEquals(date2, slots.get(2).getDate());
    }

    @Test
    void testGetSlotByIdAfterBooking() {
        // 1. Create restaurant and slot
        RestaurantDTO restaurantDTO = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Booking Restaurant")
                        .city("London")
                        .country("UK")
                        .phone("123-456-7890")
                        .email("booking@restaurant.com")
                        .active(true)
                        .build()
        );
        Long restaurantId = restaurantDTO.getId();

        TimeSlotDTO timeSlotDTO = TimeSlotDTO.builder()
                .restaurantId(restaurantId)
                .date(LocalDate.now())
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(23, 0))
                .capacity(8)
                .seatsAvailable(8)
                .build();

        TimeSlotDTO createdSlot = timeSlotService.createTimeSlot(restaurantId, timeSlotDTO);
        Long slotId = createdSlot.getId();

        // 2. Book 3 seats
        timeSlotService.bookSeats(restaurantId, slotId, 3);

        // 3. Get updated slot
        TimeSlotDTO updatedSlot = restaurantService.getSlotById(slotId);

        // 4. Verify seats were reduced
        assertEquals(5, updatedSlot.getSeatsAvailable());
        assertEquals(8, updatedSlot.getCapacity());
    }

    @Test
    void testGetSlotsByRestaurantIdWithMultipleRestaurants() {
        // 1. Create two restaurants
        RestaurantDTO restaurant1 = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Restaurant A")
                        .city("City A")
                        .country("Country A")
                        .phone("123-456-7890")
                        .email("a@restaurant.com")
                        .active(true)
                        .build()
        );

        RestaurantDTO restaurant2 = restaurantService.createRestaurant(
                RestaurantDTO.builder()
                        .name("Restaurant B")
                        .city("City B")
                        .country("Country B")
                        .phone("123-456-7890")
                        .email("b@restaurant.com")
                        .active(true)
                        .build()
        );

        // 2. Create slots for both restaurants
        TimeSlotDTO slotA = TimeSlotDTO.builder()
                .restaurantId(restaurant1.getId())
                .date(LocalDate.now())
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(22, 0))
                .capacity(10)
                .seatsAvailable(10)
                .build();
        timeSlotService.createTimeSlot(restaurant1.getId(), slotA);

        TimeSlotDTO slotB1 = TimeSlotDTO.builder()
                .restaurantId(restaurant2.getId())
                .date(LocalDate.now())
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(22, 0))
                .capacity(10)
                .seatsAvailable(10)
                .build();
        timeSlotService.createTimeSlot(restaurant2.getId(), slotB1);

        TimeSlotDTO slotB2 = TimeSlotDTO.builder()
                .restaurantId(restaurant2.getId())
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(22, 0))
                .capacity(10)
                .seatsAvailable(10)
                .build();
        timeSlotService.createTimeSlot(restaurant2.getId(), slotB2);

        // 3. Get slots for each restaurant
        List<TimeSlotDTO> slotsA = restaurantService.getSlotsByRestaurantId(restaurant1.getId());
        List<TimeSlotDTO> slotsB = restaurantService.getSlotsByRestaurantId(restaurant2.getId());

        // 4. Verify isolation
        assertEquals(1, slotsA.size());
        assertEquals(2, slotsB.size());
        assertTrue(slotsA.stream().allMatch(s -> s.getRestaurantId().equals(restaurant1.getId())));
        assertTrue(slotsB.stream().allMatch(s -> s.getRestaurantId().equals(restaurant2.getId())));
    }
}
