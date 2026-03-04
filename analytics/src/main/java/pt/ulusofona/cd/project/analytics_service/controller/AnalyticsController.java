package pt.ulusofona.cd.project.analytics_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ulusofona.cd.project.analytics_service.dto.OccupancyDTO;
import pt.ulusofona.cd.project.analytics_service.dto.PopularRestaurantDTO;
import pt.ulusofona.cd.project.analytics_service.dto.StatusDistributionDTO;
import pt.ulusofona.cd.project.analytics_service.dto.VipCustomerDTO;
import pt.ulusofona.cd.project.analytics_service.service.AnalyticsService;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin("*")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Business Intelligence API for reservation analytics and insights")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping({"/popular-restaurants", "/top-restaurants"})
    @Operation(
        summary = "Get popular restaurants",
        description = "Returns restaurant IDs ranked by total confirmed reservations (descending order). Data sourced from Kafka events only."
    )
    public ResponseEntity<List<PopularRestaurantDTO>> getPopularRestaurants() {
        List<PopularRestaurantDTO> restaurants = analyticsService.getPopularRestaurants();
        return ResponseEntity.ok(restaurants);
    }

    @GetMapping("/vip-customers")
    @Operation(
        summary = "Get VIP customers",
        description = "Returns customers ranked by total bookings (most loyal customers first)"
    )
    public ResponseEntity<List<VipCustomerDTO>> getVipCustomers() {
        List<VipCustomerDTO> customers = analyticsService.getVipCustomers();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/occupancy-by-date")
    @Operation(
        summary = "Get occupancy statistics by date",
        description = "Returns daily occupancy metrics including total guests, reservations, and average party size"
    )
    public ResponseEntity<List<OccupancyDTO>> getOccupancyByDate() {
        List<OccupancyDTO> occupancy = analyticsService.getOccupancyByDate();
        return ResponseEntity.ok(occupancy);
    }

    @GetMapping("/status-distribution")
    @Operation(
        summary = "Get reservation status distribution",
        description = "Returns the count of reservations by status: PENDING, CONFIRMED, and CANCELLED"
    )
    public ResponseEntity<StatusDistributionDTO> getStatusDistribution() {
        StatusDistributionDTO distribution = analyticsService.getStatusDistribution();
        return ResponseEntity.ok(distribution);
    }
}
