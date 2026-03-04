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
public class VipCustomerDTO {

    @JsonProperty("user_email")
    private String userEmail;

    @JsonProperty("total_bookings")
    private Long totalBookings;

    @JsonProperty("ranking")
    private Integer ranking;
}
