package pt.ulusofona.cd.project.restaurant_service.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ulusofona.cd.project.restaurant_service.dto.TimeSlotDTO;
import pt.ulusofona.cd.project.restaurant_service.entity.AvailabilitySlot;
import pt.ulusofona.cd.project.restaurant_service.entity.Restaurant;
import pt.ulusofona.cd.project.restaurant_service.repository.RestaurantRepository;
import pt.ulusofona.cd.project.restaurant_service.repository.TimeSlotRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final RestaurantRepository restaurantRepository;
    private final EntityManager entityManager;

    public List<TimeSlotDTO> getAvailability(Long restaurantId, LocalDate date) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> {
                    log.warn("Restaurant not found with ID: {}", restaurantId);
                    return new NoSuchElementException("Restaurant not found with ID: " + restaurantId);
                });

        if (!restaurant.getActive()) {
            log.info("Restaurant {} is inactive, returning empty availability", restaurantId);
            return List.of();
        }

        List<AvailabilitySlot> slots = timeSlotRepository.findByRestaurantAndDate(restaurantId, date);
        List<TimeSlotDTO> availableSlots = slots.stream()
                .filter(AvailabilitySlot::isAvailable)
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        log.info("Found {} available time slots for restaurant {} on date {}",
                availableSlots.size(), restaurantId, date);
        return availableSlots;
    }

    public TimeSlotDTO bookSeats(Long restaurantId, Long slotId, Integer seatCount) {
        AvailabilitySlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> {
                    log.warn("Availability slot not found with ID: {}", slotId);
                    return new NoSuchElementException("Availability slot not found with ID: " + slotId);
                });

        if (!slot.getRestaurant().getId().equals(restaurantId)) {
            log.warn("Availability slot {} does not belong to restaurant {}", slotId, restaurantId);
            throw new IllegalArgumentException("Availability slot does not belong to this restaurant");
        }

        int updatedRows = timeSlotRepository.bookSeats(slotId, seatCount);

        if (updatedRows == 0) {
            log.warn("Failed to book {} seats for slot {}. Not enough available seats.",
                    seatCount, slotId);
            throw new IllegalStateException("Not enough available seats for this time slot");
        }

        entityManager.flush();
        AvailabilitySlot updatedSlot = timeSlotRepository.findById(slotId).get();
        entityManager.refresh(updatedSlot);
        log.info("Successfully booked {} seats for slot ID: {}. Seats available now: {}",
                seatCount, slotId, updatedSlot.getSeatsAvailable());
        return mapToDTO(updatedSlot);
    }

    public TimeSlotDTO releaseSeats(Long slotId, Integer seatCount) {
        AvailabilitySlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> {
                    log.warn("Availability slot not found with ID: {}", slotId);
                    return new NoSuchElementException("Availability slot not found with ID: " + slotId);
                });

        int previousAvailable = slot.getSeatsAvailable();
        int updatedRows = timeSlotRepository.releaseSeats(slotId, seatCount);

        if (updatedRows == 0) {
            log.warn("Failed to release {} seats for slot {}", seatCount, slotId);
            throw new IllegalStateException("Failed to release seats for this time slot");
        }

        entityManager.flush();
        AvailabilitySlot updatedSlot = timeSlotRepository.findById(slotId).get();
        entityManager.refresh(updatedSlot);
        log.info("Successfully released {} seats for slot ID: {}. Seats available now: {} (was {})",
                seatCount, slotId, updatedSlot.getSeatsAvailable(), previousAvailable);
        return mapToDTO(updatedSlot);
    }

    public TimeSlotDTO createTimeSlot(Long restaurantId, TimeSlotDTO dto) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> {
                    log.warn("Restaurant not found with ID: {}", restaurantId);
                    return new NoSuchElementException("Restaurant not found with ID: " + restaurantId);
                });

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .restaurant(restaurant)
                .date(dto.getDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .capacity(dto.getCapacity())
                .seatsAvailable(dto.getCapacity())
                .build();

        AvailabilitySlot saved = timeSlotRepository.save(slot);
        log.info("Availability slot created with ID: {} for restaurant {} on date {}",
                saved.getId(), restaurantId, saved.getDate());
        return mapToDTO(saved);
    }

    public TimeSlotDTO updateSlot(Long slotId, TimeSlotDTO dto) {
        AvailabilitySlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> {
                    log.warn("Availability slot not found with ID: {}", slotId);
                    return new NoSuchElementException("Availability slot not found with ID: " + slotId);
                });

        // Check if there are active bookings
        int reservedSeats = slot.getCapacity() - slot.getSeatsAvailable();
        if (reservedSeats > 0) {
            log.warn("Cannot update slot {} - it has {} reserved seats", slotId, reservedSeats);
            throw new IllegalStateException("Cannot update a slot with active bookings");
        }

        // Update allowed fields
        if (dto.getDate() != null) {
            slot.setDate(dto.getDate());
        }
        if (dto.getStartTime() != null) {
            slot.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            slot.setEndTime(dto.getEndTime());
        }
        if (dto.getCapacity() != null) {
            slot.setCapacity(dto.getCapacity());
            // Reset seats available to new capacity
            slot.setSeatsAvailable(dto.getCapacity());
        }

        AvailabilitySlot updated = timeSlotRepository.save(slot);
        log.info("Availability slot {} updated successfully", slotId);
        return mapToDTO(updated);
    }

    public void deleteSlot(Long slotId) {
        AvailabilitySlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> {
                    log.warn("Availability slot not found with ID: {}", slotId);
                    return new NoSuchElementException("Availability slot not found with ID: " + slotId);
                });

        // Check if there are active bookings
        int reservedSeats = slot.getCapacity() - slot.getSeatsAvailable();
        if (reservedSeats > 0) {
            log.warn("Cannot delete slot {} - it has {} reserved seats", slotId, reservedSeats);
            throw new IllegalStateException("Cannot delete a slot with active bookings");
        }

        timeSlotRepository.deleteById(slotId);
        log.info("Availability slot {} deleted successfully", slotId);
    }

    private TimeSlotDTO mapToDTO(AvailabilitySlot slot) {
        return TimeSlotDTO.builder()
                .id(slot.getId())
                .restaurantId(slot.getRestaurant().getId())
                .date(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .capacity(slot.getCapacity())
                .seatsAvailable(slot.getSeatsAvailable())
                .createdAt(slot.getCreatedAt())
                .updatedAt(slot.getUpdatedAt())
                .build();
    }
}
