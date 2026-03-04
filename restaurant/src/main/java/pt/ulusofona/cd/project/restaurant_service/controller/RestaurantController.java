package pt.ulusofona.cd.project.restaurant_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ulusofona.cd.project.restaurant_service.dto.BookingRequestDTO;
import pt.ulusofona.cd.project.restaurant_service.dto.ReleaseRequestDTO;
import pt.ulusofona.cd.project.restaurant_service.dto.RestaurantDTO;
import pt.ulusofona.cd.project.restaurant_service.dto.TimeSlotDTO;
import pt.ulusofona.cd.project.restaurant_service.service.RestaurantService;
import pt.ulusofona.cd.project.restaurant_service.service.TimeSlotService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Slf4j
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final TimeSlotService timeSlotService;

    // Restaurant CRUD endpoints

    @PostMapping
    public ResponseEntity<RestaurantDTO> createRestaurant(@Valid @RequestBody RestaurantDTO dto) {
        log.info("Creating new restaurant: {}", dto.getName());
        RestaurantDTO created = restaurantService.createRestaurant(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<RestaurantDTO>> getAllRestaurants() {
        log.info("Getting all active restaurants");
        List<RestaurantDTO> restaurants = restaurantService.getAllActive();
        return ResponseEntity.ok(restaurants);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantDTO> getRestaurant(@PathVariable Long id) {
        log.info("Getting restaurant with ID: {}", id);
        RestaurantDTO restaurant = restaurantService.getRestaurant(id);
        return ResponseEntity.ok(restaurant);
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<List<TimeSlotDTO>> getSlotsByRestaurant(@PathVariable Long id) {
        log.info("Getting all slots for restaurant with ID: {}", id);
        List<TimeSlotDTO> slots = restaurantService.getSlotsByRestaurantId(id);
        return ResponseEntity.ok(slots);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantDTO> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantDTO dto) {
        log.info("Updating restaurant with ID: {}", id);
        RestaurantDTO updated = restaurantService.updateRestaurant(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable Long id) {
        log.info("Deleting restaurant with ID: {}", id);
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.noContent().build();
    }

    // Availability Slot endpoints

    @PostMapping("/{restaurantId}/slots")
    public ResponseEntity<TimeSlotDTO> createSlot(
            @PathVariable Long restaurantId,
            @Valid @RequestBody TimeSlotDTO dto) {
        log.info("Creating availability slot for restaurant {}", restaurantId);
        TimeSlotDTO created = timeSlotService.createTimeSlot(restaurantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<List<TimeSlotDTO>> getAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Getting availability for restaurant {} on date {}", id, date);
        List<TimeSlotDTO> availability = timeSlotService.getAvailability(id, date);
        return ResponseEntity.ok(availability);
    }

    @PostMapping("/{restaurantId}/slots/{slotId}/book")
    public ResponseEntity<TimeSlotDTO> bookSeats(
            @PathVariable Long restaurantId,
            @PathVariable Long slotId,
            @Valid @RequestBody BookingRequestDTO request) {

        log.info("Booking {} seats for slot {} in restaurant {}",
                request.getSeatCount(), slotId, restaurantId);

        try {
            TimeSlotDTO result = timeSlotService.bookSeats(restaurantId, slotId, request.getSeatCount());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Booking failed - conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/slots/release")
    public ResponseEntity<TimeSlotDTO> releaseSeats(
            @Valid @RequestBody ReleaseRequestDTO request) {

        log.info("Releasing {} seats for slot {}", request.getSeatCount(), request.getSlotId());
        TimeSlotDTO result = timeSlotService.releaseSeats(request.getSlotId(), request.getSeatCount());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/slots/{slotId}")
    public ResponseEntity<TimeSlotDTO> updateSlot(
            @PathVariable Long slotId,
            @Valid @RequestBody TimeSlotDTO dto) {

        log.info("Updating availability slot {}", slotId);
        try {
            TimeSlotDTO updated = timeSlotService.updateSlot(slotId, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            log.warn("Update failed - conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @DeleteMapping("/slots/{slotId}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long slotId) {

        log.info("Deleting availability slot {}", slotId);
        try {
            timeSlotService.deleteSlot(slotId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Delete failed - conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
