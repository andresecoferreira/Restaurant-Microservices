package pt.ulusofona.cd.project.restaurant_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ulusofona.cd.project.restaurant_service.dto.TimeSlotDTO;
import pt.ulusofona.cd.project.restaurant_service.service.RestaurantService;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
@Slf4j
public class SlotController {

    private final RestaurantService restaurantService;

    @GetMapping("/{id}")
    public ResponseEntity<TimeSlotDTO> getSlotById(@PathVariable Long id) {
        log.info("Getting slot with ID: {}", id);
        TimeSlotDTO slot = restaurantService.getSlotById(id);
        return ResponseEntity.ok(slot);
    }
}
