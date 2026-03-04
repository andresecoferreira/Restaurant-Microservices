package pt.ulusofona.cd.project.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Payload for RESERVATION_CANCELLED events.
 * Contains only the business data; metadata is handled by MessageEnvelope.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationCancelledEvent {
    @JsonProperty("reservationId")
    private UUID reservationId;

    @JsonProperty("restaurantId")
    private Long restaurantId;

    @JsonProperty("slotId")
    private Long slotId;

    @JsonProperty("userEmail")
    private String userEmail;

    @JsonProperty("partySize")
    private Integer partySize;
}
