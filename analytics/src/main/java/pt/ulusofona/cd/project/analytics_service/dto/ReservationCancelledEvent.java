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
public class ReservationCancelledEvent {

    @JsonProperty("reservationId")
    private String reservationId;

    @JsonProperty("userEmail")
    private String userEmail;

    @JsonProperty("cancelledAt")
    private Instant cancelledAt;
}
