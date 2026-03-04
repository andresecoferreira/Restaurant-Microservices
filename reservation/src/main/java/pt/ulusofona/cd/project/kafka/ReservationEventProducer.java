package pt.ulusofona.cd.project.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pt.ulusofona.cd.project.event.MessageEnvelope;
import pt.ulusofona.cd.project.event.ReservationCreatedEvent;
import pt.ulusofona.cd.project.event.ReservationConfirmedEvent;
import pt.ulusofona.cd.project.event.ReservationCancelledEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event producer for reservation events.
 * Wraps all events in MessageEnvelope with traceId, eventType, status, and occurredAt.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationEventProducer {
    private final KafkaTemplate<String, MessageEnvelope<ReservationCreatedEvent>> createdEventTemplate;
    private final KafkaTemplate<String, MessageEnvelope<ReservationConfirmedEvent>> confirmedEventTemplate;
    private final KafkaTemplate<String, MessageEnvelope<ReservationCancelledEvent>> cancelledEventTemplate;

    /**
     * Publishes a reservation created event wrapped in MessageEnvelope.
     *
     * @param event the reservation created event payload
     */
    public void publishReservationCreatedEvent(ReservationCreatedEvent event) {
        String traceId = UUID.randomUUID().toString();
        MessageEnvelope<ReservationCreatedEvent> envelope = MessageEnvelope.<ReservationCreatedEvent>builder()
            .eventType("reservation.created")
            .status("PENDING")
            .occurredAt(Instant.now())
            .traceId(traceId)
            .payload(event)
            .build();

        String key = event.getReservationId().toString();
        createdEventTemplate.send("reservation.created", key, envelope);
        log.info("Published RESERVATION_CREATED event with traceId: {}", traceId);
    }

    /**
     * Sends a reservation confirmed event to Kafka.
     *
     * @param envelope the message envelope containing the reservation confirmed event
     */
    public void sendReservationConfirmed(MessageEnvelope<ReservationConfirmedEvent> envelope) {
        String key = envelope.getPayload().getReservationId().toString();
        confirmedEventTemplate.send("reservation.confirmed", key, envelope);
        log.info("Sent RESERVATION_CONFIRMED event to Kafka with traceId: {}", envelope.getTraceId());
    }

    /**
     * Sends a reservation cancelled event to Kafka.
     *
     * @param envelope the message envelope containing the reservation cancelled event
     */
    public void sendReservationCancelled(MessageEnvelope<ReservationCancelledEvent> envelope) {
        String key = envelope.getPayload().getReservationId().toString();
        cancelledEventTemplate.send("reservation.cancelled", key, envelope);
        log.info("Sent RESERVATION_CANCELLED event to Kafka with traceId: {}", envelope.getTraceId());
    }
}
