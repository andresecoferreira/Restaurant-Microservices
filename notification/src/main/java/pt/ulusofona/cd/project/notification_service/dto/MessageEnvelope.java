package pt.ulusofona.cd.project.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Generic message envelope wrapper for all Kafka events.
 * This ensures consistent event structure across all microservices.
 *
 * @param <T> the type of payload contained in this envelope
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEnvelope<T> {

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("status")
    private String status;

    @JsonProperty("occurredAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;

    @JsonProperty("traceId")
    private String traceId;

    @JsonProperty("payload")
    private T payload;
}
