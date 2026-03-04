package pt.ulusofona.cd.project.notification_service.dto;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DTOSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testMessageEnvelopeWithNotificationPayloadSerialization() throws Exception {
        // Arrange
        String traceId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        NotificationPayload payload = NotificationPayload.builder()
            .reservationId("RES-123")
            .restaurantId(456L)
            .userEmail("user@example.com")
            .partySize(4)
            .slotId(789L)
            .build();

        MessageEnvelope<NotificationPayload> envelope = MessageEnvelope.<NotificationPayload>builder()
            .eventType("reservation.created")
            .status("SENT")
            .occurredAt(now)
            .traceId(traceId)
            .payload(payload)
            .build();

        // Act
        String json = objectMapper.writeValueAsString(envelope);
        System.out.println("Serialized JSON: " + json);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"eventType\":\"reservation.created\""));
        assertTrue(json.contains("\"status\":\"SENT\""));
        assertTrue(json.contains("\"traceId\":\"" + traceId + "\""));
        assertTrue(json.contains("\"payload\""));
        assertTrue(json.contains("\"notification_id\"") == false); // payload should have reservationId, not notification_id
        assertTrue(json.contains("\"reservationId\":\"RES-123\""));
        assertTrue(json.contains("\"userEmail\":\"user@example.com\""));
    }

    @Test
    void testMessageEnvelopeWithNotificationPayloadDeserialization() throws Exception {
        // Arrange
        String json = """
            {
              "eventType": "reservation.confirmed",
              "status": "SENT",
              "occurredAt": "2026-01-04T12:00:00Z",
              "traceId": "abc-123-def",
              "payload": {
                "reservationId": "RES-456",
                "restaurantId": 789,
                "userEmail": "customer@example.com",
                "partySize": 6,
                "slotId": 101
              }
            }
            """;

        // Act
        JavaType type = objectMapper.getTypeFactory()
            .constructParametricType(MessageEnvelope.class, NotificationPayload.class);
        MessageEnvelope<NotificationPayload> envelope = objectMapper.readValue(json, type);

        // Assert
        assertNotNull(envelope);
        assertEquals("reservation.confirmed", envelope.getEventType());
        assertEquals("SENT", envelope.getStatus());
        assertEquals("abc-123-def", envelope.getTraceId());

        NotificationPayload payload = envelope.getPayload();
        assertNotNull(payload);
        assertEquals("RES-456", payload.getReservationId());
        assertEquals(789L, payload.getRestaurantId());
        assertEquals("customer@example.com", payload.getUserEmail());
        assertEquals(6, payload.getPartySize());
        assertEquals(101L, payload.getSlotId());
    }

    @Test
    void testRestaurantNotifiedEventSerialization() throws Exception {
        // Arrange
        String traceId = UUID.randomUUID().toString();

        RestaurantNotifiedEvent.Payload auditPayload = RestaurantNotifiedEvent.Payload.builder()
            .notificationId(UUID.randomUUID().toString())
            .reservationId("RES-789")
            .relatedEventType("reservation.created")
            .recipient("user@example.com")
            .build();

        RestaurantNotifiedEvent auditEvent = RestaurantNotifiedEvent.builder()
            .eventType("restaurant.notified")
            .status("SENT")
            .occurredAt(LocalDateTime.now())
            .traceId(traceId)
            .payload(auditPayload)
            .build();

        // Act
        String json = objectMapper.writeValueAsString(auditEvent);
        System.out.println("Audit Event JSON: " + json);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"event_type\":\"restaurant.notified\""));
        assertTrue(json.contains("\"status\":\"SENT\""));
        assertTrue(json.contains("\"trace_id\":\"" + traceId + "\""));
        assertTrue(json.contains("\"related_event_type\":\"reservation.created\""));
        assertTrue(json.contains("\"reservation_id\":\"RES-789\""));
        assertTrue(json.contains("\"notification_id\""));
    }

    @Test
    void testNotificationPayloadWithAllFields() throws Exception {
        // Arrange
        String json = """
            {
              "reservationId": "RES-001",
              "restaurantId": 100,
              "userEmail": "test@email.com",
              "partySize": 2,
              "slotId": 50
            }
            """;

        // Act
        NotificationPayload payload = objectMapper.readValue(json, NotificationPayload.class);

        // Assert
        assertNotNull(payload);
        assertEquals("RES-001", payload.getReservationId());
        assertEquals(100L, payload.getRestaurantId());
        assertEquals("test@email.com", payload.getUserEmail());
        assertEquals(2, payload.getPartySize());
        assertEquals(50L, payload.getSlotId());
    }

    @Test
    void testJsonPropertyAnnotationsForCamelCase() throws Exception {
        // Arrange - Test that camelCase is properly deserialized
        String json = """
            {
              "eventType": "reservation.created",
              "status": "SENT",
              "occurredAt": "2026-01-04T12:00:00Z",
              "traceId": "trace-123",
              "payload": {
                "reservationId": "RES-999",
                "restaurantId": 777,
                "userEmail": "admin@example.com",
                "partySize": 8,
                "slotId": 200
              }
            }
            """;

        // Act
        JavaType type = objectMapper.getTypeFactory()
            .constructParametricType(MessageEnvelope.class, NotificationPayload.class);
        MessageEnvelope<NotificationPayload> envelope = objectMapper.readValue(json, type);

        // Assert - Verify camelCase was properly deserialized
        assertEquals("reservation.created", envelope.getEventType());
        assertEquals("trace-123", envelope.getTraceId());
        assertEquals("RES-999", envelope.getPayload().getReservationId());
        assertEquals("admin@example.com", envelope.getPayload().getUserEmail());
    }

    @Test
    void testRoundTripSerializationDeserialization() throws Exception {
        // Arrange
        String traceId = "trace-round-trip";
        NotificationPayload originalPayload = NotificationPayload.builder()
            .reservationId("RES-ROUNDTRIP")
            .restaurantId(999L)
            .userEmail("roundtrip@example.com")
            .partySize(3)
            .slotId(150L)
            .build();

        MessageEnvelope<NotificationPayload> originalEnvelope = MessageEnvelope.<NotificationPayload>builder()
            .eventType("reservation.cancelled")
            .status("PROCESSED")
            .occurredAt(Instant.now())
            .traceId(traceId)
            .payload(originalPayload)
            .build();

        // Act
        String serialized = objectMapper.writeValueAsString(originalEnvelope);
        JavaType type = objectMapper.getTypeFactory()
            .constructParametricType(MessageEnvelope.class, NotificationPayload.class);
        MessageEnvelope<NotificationPayload> deserializedEnvelope = objectMapper.readValue(serialized, type);

        // Assert
        assertEquals(originalEnvelope.getEventType(), deserializedEnvelope.getEventType());
        assertEquals(originalEnvelope.getStatus(), deserializedEnvelope.getStatus());
        assertEquals(originalEnvelope.getTraceId(), deserializedEnvelope.getTraceId());
        assertEquals(originalPayload.getReservationId(), deserializedEnvelope.getPayload().getReservationId());
        assertEquals(originalPayload.getUserEmail(), deserializedEnvelope.getPayload().getUserEmail());
        assertEquals(originalPayload.getRestaurantId(), deserializedEnvelope.getPayload().getRestaurantId());
    }
}
