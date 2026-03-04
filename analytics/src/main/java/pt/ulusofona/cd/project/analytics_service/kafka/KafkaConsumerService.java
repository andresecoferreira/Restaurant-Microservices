package pt.ulusofona.cd.project.analytics_service.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import pt.ulusofona.cd.project.analytics_service.dto.MessageEnvelope;
import pt.ulusofona.cd.project.analytics_service.dto.ReservationCancelledEvent;
import pt.ulusofona.cd.project.analytics_service.dto.ReservationConfirmedEvent;
import pt.ulusofona.cd.project.analytics_service.dto.ReservationCreatedEvent;
import pt.ulusofona.cd.project.analytics_service.service.AnalyticsService;

/**
 * Kafka consumer for Analytics Service.
 * Listens to core reservation events and populates analytics dashboard.
 *
 * NOTE: This service consumes from core reservation topics only.
 * The restaurant.notified topic is an audit log and should NOT be consumed here.
 * Analytics data is computed from reservation status changes (PENDING -> CONFIRMED/CANCELLED).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    /**
     * Consumes reservation.created events and stores pending reservations in analytics.
     * Uses new consumer group with "v2" suffix to force re-reading all historical messages.
     */
    @KafkaListener(
        topics = "reservation.created",
        groupId = "analytics-service-consumer-v2"
    )
    public void handleReservationCreated(String message) {
        log.info("📊 RAW ANALYTICS (reservation.created): {}", message);

        try {
            MessageEnvelope<ReservationCreatedEvent> envelope = objectMapper.readValue(
                message,
                new TypeReference<MessageEnvelope<ReservationCreatedEvent>>() {}
            );

            log.info("📊 Processing reservation.created: {} | traceId: {}",
                     envelope.getPayload().getReservationId(),
                     envelope.getTraceId());
            analyticsService.handleReservationCreated(envelope);
        } catch (Exception e) {
            log.error("❌ Failed to process reservation.created event: {}", e.getMessage(), e);
        }
    }

    /**
     * Consumes reservation.confirmed events and updates reservation status.
     * This enables occupancy, VIP customer, and popular restaurant analytics.
     */
    @KafkaListener(
        topics = "reservation.confirmed",
        groupId = "analytics-service-consumer-v2"
    )
    public void handleReservationConfirmed(String message) {
        log.info("📊 RAW ANALYTICS (reservation.confirmed): {}", message);

        try {
            MessageEnvelope<ReservationConfirmedEvent> envelope = objectMapper.readValue(
                message,
                new TypeReference<MessageEnvelope<ReservationConfirmedEvent>>() {}
            );

            log.info("✅ Processing reservation.confirmed: {} | traceId: {}",
                     envelope.getPayload().getReservationId(),
                     envelope.getTraceId());
            analyticsService.handleReservationConfirmed(envelope);
        } catch (Exception e) {
            log.error("❌ Failed to process reservation.confirmed event: {}", e.getMessage(), e);
        }
    }

    /**
     * Consumes reservation.cancelled events and updates reservation status.
     * This enables status distribution and cancellation rate analytics.
     */
    @KafkaListener(
        topics = "reservation.cancelled",
        groupId = "analytics-service-consumer-v2"
    )
    public void handleReservationCancelled(String message) {
        log.info("📊 RAW ANALYTICS (reservation.cancelled): {}", message);

        try {
            MessageEnvelope<ReservationCancelledEvent> envelope = objectMapper.readValue(
                message,
                new TypeReference<MessageEnvelope<ReservationCancelledEvent>>() {}
            );

            log.info("❌ Processing reservation.cancelled: {} | traceId: {}",
                     envelope.getPayload().getReservationId(),
                     envelope.getTraceId());
            analyticsService.handleReservationCancelled(envelope);
        } catch (Exception e) {
            log.error("❌ Failed to process reservation.cancelled event: {}", e.getMessage(), e);
        }
    }
}
