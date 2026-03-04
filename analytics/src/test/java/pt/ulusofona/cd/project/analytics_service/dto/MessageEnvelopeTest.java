package pt.ulusofona.cd.project.analytics_service.dto;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MessageEnvelope Serialization Tests")
class MessageEnvelopeTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should serialize and deserialize MessageEnvelope with ReservationCreatedEvent payload")
    void testMessageEnvelopeWithCreatedEvent() throws Exception {
        // Arrange
        String reservationId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        ReservationCreatedEvent event = new ReservationCreatedEvent(
            reservationId,
            1L,
            100L,
            "user@test.com",
            4,
            now
        );

        MessageEnvelope<ReservationCreatedEvent> original = MessageEnvelope.<ReservationCreatedEvent>builder()
            .eventType("reservation.created")
            .status("PENDING")
            .occurredAt(now)
            .traceId(traceId)
            .payload(event)
            .build();

        // Act - Serialize
        String json = objectMapper.writeValueAsString(original);

        // Assert serialization includes all fields
        assertTrue(json.contains("\"eventType\":\"reservation.created\""));
        assertTrue(json.contains("\"status\":\"PENDING\""));
        assertTrue(json.contains("\"traceId\":\"" + traceId + "\""));
        assertTrue(json.contains("\"reservation_id\":\"" + reservationId + "\""));
        assertTrue(json.contains("\"restaurant_id\":1"));
        assertTrue(json.contains("\"party_size\":4"));

        // Act - Deserialize
        JavaType envelopeType = objectMapper.getTypeFactory()
            .constructParametricType(MessageEnvelope.class, ReservationCreatedEvent.class);
        MessageEnvelope<ReservationCreatedEvent> deserialized = objectMapper.readValue(json, envelopeType);

        // Assert deserialization
        assertEquals(original.getEventType(), deserialized.getEventType());
        assertEquals(original.getStatus(), deserialized.getStatus());
        assertEquals(original.getTraceId(), deserialized.getTraceId());
        assertNotNull(deserialized.getOccurredAt());
        assertNotNull(deserialized.getPayload());
        assertEquals(reservationId, deserialized.getPayload().getReservationId());
        assertEquals(1L, deserialized.getPayload().getRestaurantId());
        assertEquals("user@test.com", deserialized.getPayload().getUserEmail());
        assertEquals(4, deserialized.getPayload().getPartySize());
    }

    @Test
    @DisplayName("Should serialize and deserialize MessageEnvelope with ReservationConfirmedEvent payload")
    void testMessageEnvelopeWithConfirmedEvent() throws Exception {
        // Arrange
        String reservationId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        ReservationConfirmedEvent event = new ReservationConfirmedEvent(
            reservationId,
            "user@test.com",
            now
        );

        MessageEnvelope<ReservationConfirmedEvent> original = MessageEnvelope.<ReservationConfirmedEvent>builder()
            .eventType("reservation.confirmed")
            .status("CONFIRMED")
            .occurredAt(now)
            .traceId(traceId)
            .payload(event)
            .build();

        // Act - Serialize
        String json = objectMapper.writeValueAsString(original);

        // Act - Deserialize
        JavaType envelopeType = objectMapper.getTypeFactory()
            .constructParametricType(MessageEnvelope.class, ReservationConfirmedEvent.class);
        MessageEnvelope<ReservationConfirmedEvent> deserialized = objectMapper.readValue(json, envelopeType);

        // Assert
        assertEquals("reservation.confirmed", deserialized.getEventType());
        assertEquals("CONFIRMED", deserialized.getStatus());
        assertEquals(traceId, deserialized.getTraceId());
        assertEquals(reservationId, deserialized.getPayload().getReservationId());
        assertEquals("user@test.com", deserialized.getPayload().getUserEmail());
    }

    @Test
    @DisplayName("Should serialize and deserialize MessageEnvelope with ReservationCancelledEvent payload")
    void testMessageEnvelopeWithCancelledEvent() throws Exception {
        // Arrange
        String reservationId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        ReservationCancelledEvent event = new ReservationCancelledEvent(
            reservationId,
            "user@test.com",
            now
        );

        MessageEnvelope<ReservationCancelledEvent> original = MessageEnvelope.<ReservationCancelledEvent>builder()
            .eventType("reservation.cancelled")
            .status("CANCELLED")
            .occurredAt(now)
            .traceId(traceId)
            .payload(event)
            .build();

        // Act - Serialize
        String json = objectMapper.writeValueAsString(original);

        // Act - Deserialize
        JavaType envelopeType = objectMapper.getTypeFactory()
            .constructParametricType(MessageEnvelope.class, ReservationCancelledEvent.class);
        MessageEnvelope<ReservationCancelledEvent> deserialized = objectMapper.readValue(json, envelopeType);

        // Assert
        assertEquals("reservation.cancelled", deserialized.getEventType());
        assertEquals("CANCELLED", deserialized.getStatus());
        assertEquals(traceId, deserialized.getTraceId());
        assertEquals(reservationId, deserialized.getPayload().getReservationId());
        assertEquals("user@test.com", deserialized.getPayload().getUserEmail());
    }

    @Test
    @DisplayName("Should handle missing optional fields in MessageEnvelope")
    void testMessageEnvelopeWithMissingOptionalFields() throws Exception {
        // Arrange - JSON with minimal required fields
        String json = "{" +
            "\"eventType\":\"reservation.created\"," +
            "\"payload\":{" +
                "\"reservation_id\":\"" + UUID.randomUUID() + "\"," +
                "\"user_email\":\"test@example.com\"" +
            "}" +
        "}";

        // Act
        JavaType envelopeType = objectMapper.getTypeFactory()
            .constructParametricType(MessageEnvelope.class, ReservationCreatedEvent.class);
        MessageEnvelope<ReservationCreatedEvent> deserialized = objectMapper.readValue(json, envelopeType);

        // Assert
        assertEquals("reservation.created", deserialized.getEventType());
        assertNull(deserialized.getStatus());  // Optional
        assertNull(deserialized.getTraceId()); // Optional
        assertNull(deserialized.getOccurredAt()); // Optional
        assertNotNull(deserialized.getPayload());
    }

    @Test
    @DisplayName("Should correctly handle payload with extra unknown fields")
    void testMessageEnvelopeWithUnknownPayloadFields() throws Exception {
        // Arrange
        String json = "{" +
            "\"eventType\":\"reservation.created\"," +
            "\"status\":\"PENDING\"," +
            "\"traceId\":\"trace-123\"," +
            "\"payload\":{" +
                "\"reservation_id\":\"res-123\"," +
                "\"restaurant_id\":1," +
                "\"slot_id\":100," +
                "\"user_email\":\"user@test.com\"," +
                "\"party_size\":4," +
                "\"created_at\":\"2026-01-04T15:00:00Z\"," +
                "\"unknown_field\":\"should_be_ignored\"" +
            "}" +
        "}";

        // Act
        JavaType envelopeType = objectMapper.getTypeFactory()
            .constructParametricType(MessageEnvelope.class, ReservationCreatedEvent.class);
        MessageEnvelope<ReservationCreatedEvent> deserialized = objectMapper.readValue(json, envelopeType);

        // Assert - Should ignore unknown field
        assertEquals("reservation.created", deserialized.getEventType());
        assertEquals("res-123", deserialized.getPayload().getReservationId());
        assertEquals("user@test.com", deserialized.getPayload().getUserEmail());
    }
}
