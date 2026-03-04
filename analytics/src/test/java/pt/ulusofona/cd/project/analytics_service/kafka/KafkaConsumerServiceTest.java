package pt.ulusofona.cd.project.analytics_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.ulusofona.cd.project.analytics_service.dto.MessageEnvelope;
import pt.ulusofona.cd.project.analytics_service.dto.ReservationCancelledEvent;
import pt.ulusofona.cd.project.analytics_service.dto.ReservationConfirmedEvent;
import pt.ulusofona.cd.project.analytics_service.dto.ReservationCreatedEvent;
import pt.ulusofona.cd.project.analytics_service.service.AnalyticsService;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kafka Consumer Service Tests")
class KafkaConsumerServiceTest {

    @Mock
    private AnalyticsService analyticsService;

    private KafkaConsumerService kafkaConsumerService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        kafkaConsumerService = new KafkaConsumerService(analyticsService, objectMapper);
    }

    @Test
    @DisplayName("Should deserialize and handle ReservationCreatedEvent from MessageEnvelope")
    void testHandleReservationCreated() throws Exception {
        // Arrange
        String reservationId = UUID.randomUUID().toString();
        ReservationCreatedEvent event = new ReservationCreatedEvent(
            reservationId,
            1L,
            100L,
            "test@example.com",
            4,
            Instant.now()
        );

        MessageEnvelope<ReservationCreatedEvent> envelope = MessageEnvelope.<ReservationCreatedEvent>builder()
            .eventType("reservation.created")
            .status("PENDING")
            .occurredAt(Instant.now())
            .traceId(UUID.randomUUID().toString())
            .payload(event)
            .build();

        String jsonMessage = objectMapper.writeValueAsString(envelope);

        // Act
        kafkaConsumerService.handleReservationCreated(jsonMessage);

        // Assert
        verify(analyticsService, times(1)).handleReservationCreated(any(MessageEnvelope.class));
    }

    @Test
    @DisplayName("Should deserialize and handle ReservationConfirmedEvent from MessageEnvelope")
    void testHandleReservationConfirmed() throws Exception {
        // Arrange
        String reservationId = UUID.randomUUID().toString();
        ReservationConfirmedEvent event = new ReservationConfirmedEvent(
            reservationId,
            "test@example.com",
            Instant.now()
        );

        MessageEnvelope<ReservationConfirmedEvent> envelope = MessageEnvelope.<ReservationConfirmedEvent>builder()
            .eventType("reservation.confirmed")
            .status("CONFIRMED")
            .occurredAt(Instant.now())
            .traceId(UUID.randomUUID().toString())
            .payload(event)
            .build();

        String jsonMessage = objectMapper.writeValueAsString(envelope);

        // Act
        kafkaConsumerService.handleReservationConfirmed(jsonMessage);

        // Assert
        verify(analyticsService, times(1)).handleReservationConfirmed(any(MessageEnvelope.class));
    }

    @Test
    @DisplayName("Should deserialize and handle ReservationCancelledEvent from MessageEnvelope")
    void testHandleReservationCancelled() throws Exception {
        // Arrange
        String reservationId = UUID.randomUUID().toString();
        ReservationCancelledEvent event = new ReservationCancelledEvent(
            reservationId,
            "test@example.com",
            Instant.now()
        );

        MessageEnvelope<ReservationCancelledEvent> envelope = MessageEnvelope.<ReservationCancelledEvent>builder()
            .eventType("reservation.cancelled")
            .status("CANCELLED")
            .occurredAt(Instant.now())
            .traceId(UUID.randomUUID().toString())
            .payload(event)
            .build();

        String jsonMessage = objectMapper.writeValueAsString(envelope);

        // Act
        kafkaConsumerService.handleReservationCancelled(jsonMessage);

        // Assert
        verify(analyticsService, times(1)).handleReservationCancelled(any(MessageEnvelope.class));
    }

    @Test
    @DisplayName("Should handle invalid JSON gracefully without throwing exception")
    void testHandleInvalidJsonGracefully() {
        // Arrange
        String invalidJson = "{ invalid json }";

        // Act & Assert - Should not throw, should log error
        kafkaConsumerService.handleReservationCreated(invalidJson);
        // Verify service was not called due to deserialization error
        verify(analyticsService, times(0)).handleReservationCreated(any());
    }
}
