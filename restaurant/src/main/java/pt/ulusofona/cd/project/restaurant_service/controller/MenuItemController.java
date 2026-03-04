package pt.ulusofona.cd.project.restaurant_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ulusofona.cd.project.restaurant_service.dto.MenuItemDTO;
import pt.ulusofona.cd.project.restaurant_service.service.MenuItemService;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Slf4j
public class MenuItemController {

    private final MenuItemService menuItemService;

    @PostMapping("/items/{restaurantId}")
    public ResponseEntity<MenuItemDTO> createMenuItem(
            @PathVariable Long restaurantId,
            @Valid @RequestBody MenuItemDTO dto) {
        log.info("Creating menu item for restaurant {}", restaurantId);
        MenuItemDTO created = menuItemService.createMenuItem(restaurantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<MenuItemDTO> getMenuItem(@PathVariable Long id) {
        log.info("Getting menu item with ID: {}", id);
        MenuItemDTO menuItem = menuItemService.getMenuItem(id);
        return ResponseEntity.ok(menuItem);
    }

    @GetMapping("/restaurants/{restaurantId}")
    public ResponseEntity<List<MenuItemDTO>> getMenuByRestaurant(@PathVariable Long restaurantId) {
        log.info("Getting menu items for restaurant {}", restaurantId);
        List<MenuItemDTO> menuItems = menuItemService.getMenuByRestaurant(restaurantId);
        return ResponseEntity.ok(menuItems);
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<MenuItemDTO> updateMenuItem(
            @PathVariable Long id,
            @Valid @RequestBody MenuItemDTO dto) {
        log.info("Updating menu item with ID: {}", id);
        MenuItemDTO updated = menuItemService.updateMenuItem(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        log.info("Deleting menu item with ID: {}", id);
        menuItemService.deleteMenuItem(id);
        return ResponseEntity.noContent().build();
    }
}
