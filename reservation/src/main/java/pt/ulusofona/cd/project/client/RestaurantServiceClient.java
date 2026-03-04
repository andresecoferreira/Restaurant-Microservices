package pt.ulusofona.cd.project.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import pt.ulusofona.cd.project.dto.*;
import java.util.List;

/**
 * OpenFeign client for Restaurant Service API.
 *
 * Exposed endpoints:
 * - GET /api/restaurants/{id}
 * - GET /api/slots/{id}
 * - GET /api/restaurants/{id}/slots
 * - POST /api/restaurants/{restaurantId}/slots/{slotId}/book
 * - POST /api/restaurants/slots/release
 */
@FeignClient(name = "restaurant-service", url = "http://restaurant-service:8081")
public interface RestaurantServiceClient {

    /**
     * Get a single slot by ID with capacity and availability info.
     *
     * @param slotId the slot ID
     * @return slot details (id, date, startTime, endTime, capacity, seatsAvailable, etc.)
     */
    @GetMapping("/api/slots/{slotId}")
    TimeSlotDTO getSlotById(@PathVariable("slotId") Long slotId);

    /**
     * Get all available slots for a restaurant.
     *
     * @param restaurantId the restaurant ID
     * @return list of all slots for the restaurant
     */
    @GetMapping("/api/restaurants/{restaurantId}/slots")
    List<TimeSlotDTO> getRestaurantSlots(@PathVariable("restaurantId") Long restaurantId);

    /**
     * Book seats in a specific time slot.
     *
     * @param restaurantId the restaurant ID
     * @param slotId       the slot ID
     * @param request      booking request with seatCount
     * @return updated slot details after booking
     */
    @PostMapping("/api/restaurants/{restaurantId}/slots/{slotId}/book")
    TimeSlotDTO bookSeats(@PathVariable("restaurantId") Long restaurantId, @PathVariable("slotId") Long slotId, @RequestBody BookingRequestDTO request);

    /**
     * Release previously booked seats back to a slot.
     *
     * @param request release request with slotId and seatCount
     * @return updated slot details after release
     */
    @PostMapping("/api/restaurants/slots/release")
    TimeSlotDTO releaseSeats(@RequestBody ReleaseRequestDTO request);
}
