package pt.ulusofona.cd.project.restaurant_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ulusofona.cd.project.restaurant_service.dto.RestaurantDTO;
import pt.ulusofona.cd.project.restaurant_service.dto.TimeSlotDTO;
import pt.ulusofona.cd.project.restaurant_service.entity.Restaurant;
import pt.ulusofona.cd.project.restaurant_service.repository.RestaurantRepository;
import pt.ulusofona.cd.project.restaurant_service.repository.TimeSlotRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final TimeSlotRepository timeSlotRepository;

    public RestaurantDTO createRestaurant(RestaurantDTO dto) {
        Restaurant restaurant = Restaurant.builder()
                .name(dto.getName())
                .city(dto.getCity())
                .country(dto.getCountry())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .active(true)
                .build();
        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant created with ID: {}, name: {}", saved.getId(), saved.getName());
        return mapToDTO(saved);
    }

    public RestaurantDTO getRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Restaurant not found with ID: {}", id);
                    return new NoSuchElementException("Restaurant not found with ID: " + id);
                });
        return mapToDTO(restaurant);
    }

    public List<RestaurantDTO> getAllActive() {
        return restaurantRepository.findAllActive().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public RestaurantDTO updateRestaurant(Long id, RestaurantDTO dto) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Restaurant not found with ID: {}", id);
                    return new NoSuchElementException("Restaurant not found with ID: " + id);
                });

        if (dto.getName() != null) {
            restaurant.setName(dto.getName());
        }
        if (dto.getCity() != null) {
            restaurant.setCity(dto.getCity());
        }
        if (dto.getCountry() != null) {
            restaurant.setCountry(dto.getCountry());
        }
        if (dto.getPhone() != null) {
            restaurant.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            restaurant.setEmail(dto.getEmail());
        }
        if (dto.getActive() != null) {
            restaurant.setActive(dto.getActive());
        }

        Restaurant updated = restaurantRepository.save(restaurant);
        log.info("Restaurant updated with ID: {}", updated.getId());
        return mapToDTO(updated);
    }

    public void deleteRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Restaurant not found with ID: {}", id);
                    return new NoSuchElementException("Restaurant not found with ID: " + id);
                });
        restaurant.setActive(false);
        restaurantRepository.save(restaurant);
        log.info("Restaurant soft-deleted with ID: {}", id);
    }

    public TimeSlotDTO getSlotById(Long slotId) {
        return timeSlotRepository.findById(slotId)
                .map(this::mapSlotToDTO)
                .orElseThrow(() -> {
                    log.warn("Slot not found with ID: {}", slotId);
                    return new NoSuchElementException("Slot not found with ID: " + slotId);
                });
    }

    public List<TimeSlotDTO> getSlotsByRestaurantId(Long restaurantId) {
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> {
                    log.warn("Restaurant not found with ID: {}", restaurantId);
                    return new NoSuchElementException("Restaurant not found with ID: " + restaurantId);
                });

        return timeSlotRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapSlotToDTO)
                .collect(Collectors.toList());
    }

    private RestaurantDTO mapToDTO(Restaurant restaurant) {
        return RestaurantDTO.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .city(restaurant.getCity())
                .country(restaurant.getCountry())
                .phone(restaurant.getPhone())
                .email(restaurant.getEmail())
                .active(restaurant.getActive())
                .createdAt(restaurant.getCreatedAt())
                .updatedAt(restaurant.getUpdatedAt())
                .build();
    }

    private TimeSlotDTO mapSlotToDTO(pt.ulusofona.cd.project.restaurant_service.entity.AvailabilitySlot slot) {
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
