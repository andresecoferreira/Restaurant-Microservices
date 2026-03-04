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
public class RestaurantNotifiedEvent {

    @JsonProperty("notification_id")
    private String notificationId;

    @JsonProperty("reservation_id")
    private String reservationId;

    @JsonProperty("related_event_type")
    private String relatedEventType;

    @JsonProperty("recipient")
    private String recipient;
}
