package pt.ulusofona.cd.project.restaurant_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ulusofona.cd.project.restaurant_service.dto.MenuItemDTO;
import pt.ulusofona.cd.project.restaurant_service.entity.MenuItem;
import pt.ulusofona.cd.project.restaurant_service.entity.Restaurant;
import pt.ulusofona.cd.project.restaurant_service.repository.MenuItemRepository;
import pt.ulusofona.cd.project.restaurant_service.repository.RestaurantRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    public MenuItemDTO createMenuItem(Long restaurantId, MenuItemDTO dto) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> {
                    log.warn("Restaurant not found with ID: {}", restaurantId);
                    return new NoSuchElementException("Restaurant not found with ID: " + restaurantId);
                });

        MenuItem menuItem = MenuItem.builder()
                .restaurant(restaurant)
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .currency(dto.getCurrency())
                .build();

        MenuItem saved = menuItemRepository.save(menuItem);
        log.info("Menu item created with ID: {}, name: {} for restaurant {}",
                saved.getId(), saved.getName(), restaurantId);
        return mapToDTO(saved);
    }

    public MenuItemDTO getMenuItem(Long id) {
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Menu item not found with ID: {}", id);
                    return new NoSuchElementException("Menu item not found with ID: " + id);
                });
        return mapToDTO(menuItem);
    }

    public List<MenuItemDTO> getMenuByRestaurant(Long restaurantId) {
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> {
                    log.warn("Restaurant not found with ID: {}", restaurantId);
                    return new NoSuchElementException("Restaurant not found with ID: " + restaurantId);
                });

        return menuItemRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<MenuItemDTO> getAllMenuItemsByRestaurant(Long restaurantId) {
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> {
                    log.warn("Restaurant not found with ID: {}", restaurantId);
                    return new NoSuchElementException("Restaurant not found with ID: " + restaurantId);
                });

        return menuItemRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public MenuItemDTO updateMenuItem(Long id, MenuItemDTO dto) {
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Menu item not found with ID: {}", id);
                    return new NoSuchElementException("Menu item not found with ID: " + id);
                });

        if (dto.getName() != null) {
            menuItem.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            menuItem.setDescription(dto.getDescription());
        }
        if (dto.getPrice() != null) {
            menuItem.setPrice(dto.getPrice());
        }
        if (dto.getCurrency() != null) {
            menuItem.setCurrency(dto.getCurrency());
        }

        MenuItem updated = menuItemRepository.save(menuItem);
        log.info("Menu item updated with ID: {}", updated.getId());
        return mapToDTO(updated);
    }

    public void deleteMenuItem(Long id) {
        menuItemRepository.deleteById(id);
        log.info("Menu item deleted with ID: {}", id);
    }

    private MenuItemDTO mapToDTO(MenuItem menuItem) {
        return MenuItemDTO.builder()
                .id(menuItem.getId())
                .restaurantId(menuItem.getRestaurant().getId())
                .name(menuItem.getName())
                .description(menuItem.getDescription())
                .price(menuItem.getPrice())
                .currency(menuItem.getCurrency())
                .createdAt(menuItem.getCreatedAt())
                .updatedAt(menuItem.getUpdatedAt())
                .build();
    }
}
