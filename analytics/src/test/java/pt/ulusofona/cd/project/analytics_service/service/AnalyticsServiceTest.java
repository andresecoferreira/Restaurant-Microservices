package pt.ulusofona.cd.project.analytics_service.service;

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
import pt.ulusofona.cd.project.analytics_service.model.AnalyticsReservation;
import pt.ulusofona.cd.project.analytics_service.model.ReservationStatus;
import pt.ulusofona.cd.project.analytics_service.repository.AnalyticsReservationRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Analytics Service Unit Tests")
class AnalyticsServiceTest {

    @Mock
    private AnalyticsReservationRepository repository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(repository);
    }

    @Test
    @DisplayName("Should create PENDING reservation when handling ReservationCreatedEvent")
    void testHandleReservationCreated_SavesPending() {
        // Arrange
        String reservationId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        ReservationCreatedEvent event = new ReservationCreatedEvent(
            reservationId,
            1L,
            100L,
            "user@test.com",
            4,
            now
        );

        MessageEnvelope<ReservationCreatedEvent> envelope = MessageEnvelope.<ReservationCreatedEvent>builder()
            .eventType("reservation.created")
            .status("PENDING")
            .occurredAt(now)
            .traceId(UUID.randomUUID().toString())
            .payload(event)
            .build();

        // Act
        analyticsService.handleReservationCreated(envelope);

        // Assert
        verify(repository, times(1)).save(argThat(reservation ->
            reservation.getStatus() == ReservationStatus.PENDING &&
            reservation.getRestaurantId() == 1L &&
            reservation.getUserEmail().equals("user@test.com") &&
            reservation.getPartySize() == 4
        ));
    }

    @Test
    @DisplayName("Should update reservation to CONFIRMED when handling ReservationConfirmedEvent")
    void testHandleReservationConfirmed_UpdatesStatus() {
        // Arrange
        String reservationId = UUID.randomUUID().toString();
        UUID id = UUID.fromString(reservationId);
        Instant now = Instant.now();

        AnalyticsReservation existingReservation = AnalyticsReservation.builder()
            .id(id)
            .restaurantId(1L)
            .userEmail("user@test.com")
            .partySize(4)
            .reservationDate(LocalDate.now())
            .status(ReservationStatus.PENDING)
            .build();

        when(repository.findById(id)).thenReturn(Optional.of(existingReservation));

        ReservationConfirmedEvent event = new ReservationConfirmedEvent(
            reservationId,
            "user@test.com",
            now
        );

        MessageEnvelope<ReservationConfirmedEvent> envelope = MessageEnvelope.<ReservationConfirmedEvent>builder()
            .eventType("reservation.confirmed")
            .status("CONFIRMED")
            .occurredAt(now)
            .traceId(UUID.randomUUID().toString())
            .payload(event)
            .build();

        // Act
        analyticsService.handleReservationConfirmed(envelope);

        // Assert
        verify(repository, times(1)).findById(id);
        verify(repository, times(1)).save(argThat(reservation ->
            reservation.getStatus() == ReservationStatus.CONFIRMED
        ));
    }

    @Test
    @DisplayName("Should update reservation to CANCELLED when handling ReservationCancelledEvent")
    void testHandleReservationCancelled_UpdatesStatus() {
        // Arrange
        String reservationId = UUID.randomUUID().toString();
        UUID id = UUID.fromString(reservationId);
        Instant now = Instant.now();

        AnalyticsReservation existingReservation = AnalyticsReservation.builder()
            .id(id)
            .restaurantId(1L)
            .userEmail("user@test.com")
            .partySize(4)
            .reservationDate(LocalDate.now())
            .status(ReservationStatus.PENDING)
            .build();

        when(repository.findById(id)).thenReturn(Optional.of(existingReservation));

        ReservationCancelledEvent event = new ReservationCancelledEvent(
            reservationId,
            "user@test.com",
            now
        );

        MessageEnvelope<ReservationCancelledEvent> envelope = MessageEnvelope.<ReservationCancelledEvent>builder()
            .eventType("reservation.cancelled")
            .status("CANCELLED")
            .occurredAt(now)
            .traceId(UUID.randomUUID().toString())
            .payload(event)
            .build();

        // Act
        analyticsService.handleReservationCancelled(envelope);

        // Assert
        verify(repository, times(1)).findById(id);
        verify(repository, times(1)).save(argThat(reservation ->
            reservation.getStatus() == ReservationStatus.CANCELLED
        ));
    }

    @Test
    @DisplayName("Should handle cancellation of non-existent reservation gracefully")
    void testHandleReservationCancelled_NotFound() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();
        UUID id = UUID.fromString(nonExistentId);

        when(repository.findById(id)).thenReturn(Optional.empty());

        ReservationCancelledEvent event = new ReservationCancelledEvent(
            nonExistentId,
            "user@test.com",
            Instant.now()
        );

        MessageEnvelope<ReservationCancelledEvent> envelope = MessageEnvelope.<ReservationCancelledEvent>builder()
            .eventType("reservation.cancelled")
            .status("CANCELLED")
            .occurredAt(Instant.now())
            .traceId(UUID.randomUUID().toString())
            .payload(event)
            .build();

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> analyticsService.handleReservationCancelled(envelope));
        verify(repository, times(0)).save(any());
    }
}
