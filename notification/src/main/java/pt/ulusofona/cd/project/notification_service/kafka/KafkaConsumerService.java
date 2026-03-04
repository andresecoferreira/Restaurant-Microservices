package pt.ulusofona.cd.project.notification_service.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.ulusofona.cd.project.notification_service.dto.MessageEnvelope;
import pt.ulusofona.cd.project.notification_service.dto.NotificationPayload;
import pt.ulusofona.cd.project.notification_service.dto.RestaurantNotifiedEvent;
import pt.ulusofona.cd.project.notification_service.model.Notification;
import pt.ulusofona.cd.project.notification_service.repository.NotificationRepository;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final NotificationRepository notificationRepository;
    private final KafkaTemplate<String, RestaurantNotifiedEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Unified Kafka listener for all reservation events.
     * Receives messages as raw strings and deserializes them to MessageEnvelope<NotificationPayload>.
     * This ensures proper handling of the envelope structure and traceId for audit purposes.
     */
    @KafkaListener(
        topics = {"reservation.created", "reservation.confirmed", "reservation.cancelled"},
        groupId = "notification-service"
    )
    public void handleReservationEvent(String message) {
        log.info("📨 RAW KAFKA MESSAGE RECEIVED: {}", message);
        
        try {
            // Manually parse JSON string using ObjectMapper with TypeReference
            MessageEnvelope<NotificationPayload> envelope = objectMapper.readValue(
                message, 
                new TypeReference<MessageEnvelope<NotificationPayload>>() {}
            );

            // Extract data from envelope
            NotificationPayload payload = envelope.getPayload();
            String traceId = envelope.getTraceId();
            String eventType = envelope.getEventType();

            log.info("Processing {} event with traceId: {} for reservation: {}",
                     eventType, traceId, payload.getReservationId());

            // Save notification to database
            String notificationType = mapEventTypeToNotificationType(eventType);
            Notification notification = Notification.builder()
                .reservationId(payload.getReservationId())
                .eventType(notificationType)
                .recipient(payload.getUserEmail())
                .status("SENT")
                .sentAt(LocalDateTime.now())
                .build();

            Notification savedNotification = notificationRepository.save(notification);
            log.info("✅ Notification saved for reservation {} (traceId: {})",
                     payload.getReservationId(), traceId);

            // Publish audit event with same traceId
            publishAuditEvent(savedNotification, eventType, traceId);

        } catch (Exception e) {
            log.error("❌ Failed to process reservation event: {}", e.getMessage(), e);
        }
    }

    /**
     * Maps Kafka event type to notification type for database storage.
     */
    private String mapEventTypeToNotificationType(String eventType) {
        return switch (eventType) {
            case "reservation.created" -> "CREATED";
            case "reservation.confirmed" -> "CONFIRMED";
            case "reservation.cancelled" -> "CANCELLED";
            default -> "UNKNOWN";
        };
    }

    /**
     * Publishes an audit event to the restaurant.notified topic after successfully saving a notification.
     * Uses the traceId from the incoming envelope to maintain event correlation across the system.
     *
     * @param notification the saved notification entity
     * @param relatedEventType the type of the triggering reservation event (e.g., "reservation.created")
     * @param traceId the correlation ID from the incoming envelope
     */
    private void publishAuditEvent(Notification notification, String relatedEventType, String traceId) {
        RestaurantNotifiedEvent auditEvent = RestaurantNotifiedEvent.builder()
            .eventType("restaurant.notified")
            .status("SENT")
            .occurredAt(LocalDateTime.now())
            .traceId(traceId)
            .payload(RestaurantNotifiedEvent.Payload.builder()
                .notificationId(notification.getId().toString())
                .reservationId(notification.getReservationId())
                .relatedEventType(relatedEventType)
                .recipient(notification.getRecipient())
                .build())
            .build();

        kafkaTemplate.send("restaurant.notified", notification.getReservationId(), auditEvent);
        log.info("📤 Published audit event to restaurant.notified topic for reservation {} (traceId: {})",
                 notification.getReservationId(), traceId);
    }
}
