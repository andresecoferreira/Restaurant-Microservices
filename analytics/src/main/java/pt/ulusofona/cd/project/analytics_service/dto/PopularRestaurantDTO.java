package pt.ulusofona.cd.project.analytics_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopularRestaurantDTO {

    @JsonProperty("restaurant_id")
    private Long restaurantId;

    @JsonProperty("total_reservations")
    private Long totalReservations;
}
