package pt.ulusofona.cd.project.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload data contained within the MessageEnvelope for reservation events.
 * This represents the actual event data (reservation information).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPayload {

    @JsonProperty("reservationId")
    private String reservationId;

    @JsonProperty("restaurantId")
    private Long restaurantId;

    @JsonProperty("userEmail")
    private String userEmail;

    @JsonProperty("partySize")
    private Integer partySize;

    @JsonProperty("slotId")
    private Long slotId;
}
