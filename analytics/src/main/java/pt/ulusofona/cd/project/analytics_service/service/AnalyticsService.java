package pt.ulusofona.cd.project.analytics_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ulusofona.cd.project.analytics_service.dto.MessageEnvelope;
import pt.ulusofona.cd.project.analytics_service.dto.OccupancyDTO;
import pt.ulusofona.cd.project.analytics_service.dto.PopularRestaurantDTO;
import pt.ulusofona.cd.project.analytics_service.dto.ReservationCancelledEvent;
import pt.ulusofona.cd.project.analytics_service.dto.ReservationConfirmedEvent;
import pt.ulusofona.cd.project.analytics_service.dto.ReservationCreatedEvent;
import pt.ulusofona.cd.project.analytics_service.dto.RestaurantNotifiedEvent;
import pt.ulusofona.cd.project.analytics_service.dto.StatusDistributionDTO;
import pt.ulusofona.cd.project.analytics_service.dto.VipCustomerDTO;
import pt.ulusofona.cd.project.analytics_service.model.AnalyticsReservation;
import pt.ulusofona.cd.project.analytics_service.model.ReservationStatus;
import pt.ulusofona.cd.project.analytics_service.repository.AnalyticsReservationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsReservationRepository reservationRepository;

    // Event Handlers

    @Transactional
    public void handleReservationCreated(MessageEnvelope<ReservationCreatedEvent> envelope) {
        ReservationCreatedEvent event = envelope.getPayload();

        AnalyticsReservation reservation = AnalyticsReservation.builder()
            .id(UUID.fromString(event.getReservationId()))
            .restaurantId(event.getRestaurantId())
            .userEmail(event.getUserEmail())
            .partySize(event.getPartySize())
            .reservationDate(extractReservationDate(event, envelope))
            .status(ReservationStatus.PENDING)
            .eventTimestamp(envelope.getOccurredAt() != null ?
                LocalDateTime.ofInstant(envelope.getOccurredAt(), ZoneId.systemDefault()) :
                LocalDateTime.ofInstant(event.getCreatedAt(), ZoneId.systemDefault()))
            .build();

        reservationRepository.save(reservation);
        log.info("Stored PENDING reservation {} for restaurant {} | traceId: {}",
            reservation.getId(), reservation.getRestaurantId(), envelope.getTraceId());
    }

    @Transactional
    public void handleReservationCancelled(MessageEnvelope<ReservationCancelledEvent> envelope) {
        ReservationCancelledEvent event = envelope.getPayload();
        UUID reservationId = UUID.fromString(event.getReservationId());

        reservationRepository.findById(reservationId).ifPresentOrElse(
            reservation -> {
                reservation.setStatus(ReservationStatus.CANCELLED);
                reservation.setEventTimestamp(envelope.getOccurredAt() != null ?
                    LocalDateTime.ofInstant(envelope.getOccurredAt(), ZoneId.systemDefault()) :
                    LocalDateTime.ofInstant(event.getCancelledAt(), ZoneId.systemDefault()));
                reservationRepository.save(reservation);
                log.info("Updated reservation {} to CANCELLED | traceId: {}", reservationId, envelope.getTraceId());
            },
            () -> log.warn("Reservation {} not found for cancellation", reservationId)
        );
    }

    @Transactional
    public void handleReservationConfirmed(MessageEnvelope<ReservationConfirmedEvent> envelope) {
        ReservationConfirmedEvent event = envelope.getPayload();
        UUID id = UUID.fromString(event.getReservationId());

        reservationRepository.findById(id).ifPresentOrElse(
            reservation -> {
                reservation.setStatus(ReservationStatus.CONFIRMED);
                reservation.setEventTimestamp(envelope.getOccurredAt() != null ?
                    LocalDateTime.ofInstant(envelope.getOccurredAt(), ZoneId.systemDefault()) :
                    LocalDateTime.now());
                reservationRepository.save(reservation);
                log.info("Updated reservation {} to CONFIRMED | traceId: {}", id, envelope.getTraceId());
            },
            () -> log.warn("Reservation {} not found for confirmation", id)
        );
    }

    @Transactional
    public void handleRestaurantNotified(MessageEnvelope<RestaurantNotifiedEvent> envelope) {
        RestaurantNotifiedEvent event = envelope.getPayload();
        String reservationId = event.getReservationId();
        String relatedEventType = event.getRelatedEventType();
        
        log.info("📊 Processing restaurant.notified event for reservation {} | relatedEvent: {} | traceId: {}",
            reservationId, relatedEventType, envelope.getTraceId());
        
        // Analytics dashboard is refreshed by queries on the reservation table
        // This event confirms notification was sent successfully
        // We can add additional analytics tracking here if needed
    }

    // BI Endpoints

    public List<PopularRestaurantDTO> getPopularRestaurants() {
        List<Object[]> results = reservationRepository.findPopularRestaurants(ReservationStatus.CONFIRMED);

        return results.stream()
            .map(row -> {
                Long restaurantId = (Long) row[0];
                Long count = (Long) row[1];

                return PopularRestaurantDTO.builder()
                    .restaurantId(restaurantId)
                    .totalReservations(count)
                    .build();
            })
            .collect(Collectors.toList());
    }

    public List<VipCustomerDTO> getVipCustomers() {
        List<Object[]> results = reservationRepository.findVipCustomers(ReservationStatus.CONFIRMED);

        AtomicInteger ranking = new AtomicInteger(1);
        return results.stream()
            .map(row -> VipCustomerDTO.builder()
                .userEmail((String) row[0])
                .totalBookings((Long) row[1])
                .ranking(ranking.getAndIncrement())
                .build())
            .collect(Collectors.toList());
    }

    public List<OccupancyDTO> getOccupancyByDate() {
        List<Object[]> results = reservationRepository.findOccupancyByDate(ReservationStatus.CONFIRMED);

        return results.stream()
            .map(row -> {
                LocalDate date = (LocalDate) row[0];
                Long totalGuests = (Long) row[1];
                Long totalReservations = (Long) row[2];
                Double avgPartySize = totalGuests.doubleValue() / totalReservations.doubleValue();

                return OccupancyDTO.builder()
                    .date(date)
                    .totalGuests(totalGuests)
                    .totalReservations(totalReservations)
                    .averagePartySize(Math.round(avgPartySize * 100.0) / 100.0)
                    .build();
            })
            .collect(Collectors.toList());
    }

    public StatusDistributionDTO getStatusDistribution() {
        Long pendingCount = reservationRepository.countByStatus(ReservationStatus.PENDING);
        Long confirmedCount = reservationRepository.countByStatus(ReservationStatus.CONFIRMED);
        Long cancelledCount = reservationRepository.countByStatus(ReservationStatus.CANCELLED);

        return StatusDistributionDTO.builder()
            .pending(pendingCount)
            .confirmed(confirmedCount)
            .cancelled(cancelledCount)
            .build();
    }

    // Helper Methods

    private LocalDate extractReservationDate(ReservationCreatedEvent event, MessageEnvelope<ReservationCreatedEvent> envelope) {
        // Try to use event creation date, fall back to envelope occurred date
        if (event.getCreatedAt() != null) {
            return LocalDate.ofInstant(event.getCreatedAt(), ZoneId.systemDefault());
        } else if (envelope.getOccurredAt() != null) {
            return LocalDate.ofInstant(envelope.getOccurredAt(), ZoneId.systemDefault());
        } else {
            return LocalDate.now();
        }
    }
}
