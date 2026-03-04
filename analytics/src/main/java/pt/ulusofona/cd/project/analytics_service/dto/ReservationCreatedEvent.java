package pt.ulusofona.cd.project.analytics_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReservationCreatedEvent {

    @JsonProperty("reservationId")
    private String reservationId;

    @JsonProperty("restaurantId")
    private Long restaurantId;

    @JsonProperty("slotId")
    private Long slotId;

    @JsonProperty("userEmail")
    private String userEmail;

    @JsonProperty("partySize")
    private Integer partySize;

    @JsonProperty("createdAt")
    private Instant createdAt;
}
