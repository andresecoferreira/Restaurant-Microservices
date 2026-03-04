package pt.ulusofona.cd.project.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ulusofona.cd.project.client.RestaurantServiceClient;
import pt.ulusofona.cd.project.dto.BookingRequestDTO;
import pt.ulusofona.cd.project.dto.ReleaseRequestDTO;
import pt.ulusofona.cd.project.dto.TimeSlotDTO;
import pt.ulusofona.cd.project.event.MessageEnvelope;
import pt.ulusofona.cd.project.event.ReservationCancelledEvent;
import pt.ulusofona.cd.project.event.ReservationConfirmedEvent;
import pt.ulusofona.cd.project.event.ReservationCreatedEvent;
import pt.ulusofona.cd.project.kafka.ReservationEventProducer;
import pt.ulusofona.cd.project.model.Reservation;
import pt.ulusofona.cd.project.repository.ReservationRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantServiceClient restaurantServiceClient;
    private final ReservationEventProducer eventProducer;

    @Transactional
    public Reservation createReservation(CreateReservationRequest request) {
        log.info("Creating reservation for restaurant {}, slot {}, user {}",
                request.getRestaurantId(), request.getSlotId(), request.getUserEmail());

        TimeSlotDTO slot;
        try {
            // Step 1: Fetch and validate the slot
            log.info("Fetching slot details for slot {}", request.getSlotId());
            slot = restaurantServiceClient.getSlotById(request.getSlotId());

            if (slot == null) {
                log.warn("Slot {} not found", request.getSlotId());
                throw new IllegalArgumentException("Slot not found");
            }

            log.info("Slot found: date={}, seatsAvailable={}, capacity={}",
                    slot.getDate(), slot.getSeatsAvailable(), slot.getCapacity());

            // Business Rule: Prevent booking past dates
            if (slot.getDate().isBefore(java.time.LocalDate.now())) {
                log.warn("Attempted to book past date: {}", slot.getDate());
                throw new IllegalArgumentException("Cannot book reservations for past dates");
            }

            // Business Rule: Validate party size against available seats
            if (slot.getSeatsAvailable() < request.getPartySize()) {
                log.warn("Insufficient seats available. Requested: {}, Available: {}",
                        request.getPartySize(), slot.getSeatsAvailable());
                throw new IllegalStateException(String.format(
                    "Insufficient seats available. Requested: %d, Available: %d",
                    request.getPartySize(), slot.getSeatsAvailable()
                ));
            }

            // Step 2: Book the seats
            log.info("Booking {} seats for restaurant {}, slot {}", request.getPartySize(),
                    request.getRestaurantId(), request.getSlotId());
            BookingRequestDTO bookingRequest = new BookingRequestDTO(request.getPartySize());
            slot = restaurantServiceClient.bookSeats(request.getRestaurantId(), request.getSlotId(), bookingRequest);
            log.info("Successfully booked seats for slot {} on date {}", slot.getId(), slot.getDate());

        } catch (FeignException.NotFound e) {
            log.error("Slot or restaurant not found (404): {}", e.getMessage());
            throw new IllegalArgumentException("Slot or restaurant does not exist", e);
        } catch (FeignException.Conflict e) {
            log.error("Conflict while booking seats (409): {}", e.getMessage());
            throw new IllegalStateException("Failed to book seats due to conflict - slot may be full", e);
        } catch (FeignException.BadRequest e) {
            log.error("Bad request while booking seats (400): {}", e.getMessage());
            throw new IllegalArgumentException("Invalid booking request - check party size", e);
        } catch (FeignException e) {
            log.error("Unexpected error communicating with restaurant service: {}", e.getMessage());
            throw new IllegalStateException("Failed to communicate with restaurant service", e);
        }

        // Step 3: Save Reservation as PENDING with the actual slot date
        log.info("Saving reservation with status PENDING for date {}", slot.getDate());
        Reservation reservation = new Reservation();
        reservation.setRestaurantId(request.getRestaurantId());
        reservation.setSlotId(request.getSlotId());
        reservation.setUserEmail(request.getUserEmail());
        reservation.setPartySize(request.getPartySize());
        reservation.setStatus(Reservation.ReservationStatus.PENDING);
        reservation.setCreatedAt(Instant.now());

        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Reservation created with id: {} for slot date: {}", savedReservation.getId(), slot.getDate());

        // Step 4: Publish ReservationCreatedEvent to Kafka
        log.info("Publishing ReservationCreatedEvent to Kafka");
        ReservationCreatedEvent event = ReservationCreatedEvent.builder()
                .reservationId(savedReservation.getId())
                .restaurantId(savedReservation.getRestaurantId())
                .slotId(savedReservation.getSlotId())
                .userEmail(savedReservation.getUserEmail())
                .partySize(savedReservation.getPartySize())
                .build();
        eventProducer.publishReservationCreatedEvent(event);

        return savedReservation;
    }

    @Transactional
    public Reservation confirmReservation(UUID reservationId) {
        log.info("Confirming reservation with id: {}", reservationId);
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (reservation.getStatus() != Reservation.ReservationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be confirmed");
        }

        reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
        Reservation confirmedReservation = reservationRepository.save(reservation);

        // Build ReservationConfirmedEvent and wrap in MessageEnvelope
        log.info("Publishing ReservationConfirmedEvent to Kafka");
        ReservationConfirmedEvent event = ReservationConfirmedEvent.builder()
                .reservationId(confirmedReservation.getId())
                .restaurantId(confirmedReservation.getRestaurantId())
                .userEmail(confirmedReservation.getUserEmail())
                .build();
        
        String traceId = UUID.randomUUID().toString();
        MessageEnvelope<ReservationConfirmedEvent> envelope = MessageEnvelope.<ReservationConfirmedEvent>builder()
                .eventType("reservation.confirmed")
                .status("CONFIRMED")
                .occurredAt(Instant.now())
                .traceId(traceId)
                .payload(event)
                .build();
        
        eventProducer.sendReservationConfirmed(envelope);

        return confirmedReservation;
    }

    @Transactional
    public Reservation cancelReservation(UUID reservationId) {
        log.info("Cancelling reservation with id: {}", reservationId);
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        // Release seats back to Restaurant Service
        log.info("Releasing {} seats for slot {}", reservation.getPartySize(), reservation.getSlotId());
        try {
            ReleaseRequestDTO releaseRequest = new ReleaseRequestDTO(
                    reservation.getSlotId(),
                    reservation.getPartySize()
            );
            restaurantServiceClient.releaseSeats(releaseRequest);
        } catch (FeignException e) {
            log.error("Failed to release seats", e);
            throw new IllegalStateException("Failed to release seats", e);
        }

        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        Reservation cancelledReservation = reservationRepository.save(reservation);

        // Build ReservationCancelledEvent and wrap in MessageEnvelope
        log.info("Publishing ReservationCancelledEvent to Kafka");
        ReservationCancelledEvent event = ReservationCancelledEvent.builder()
                .reservationId(cancelledReservation.getId())
                .restaurantId(cancelledReservation.getRestaurantId())
                .slotId(cancelledReservation.getSlotId())
                .userEmail(cancelledReservation.getUserEmail())
                .partySize(cancelledReservation.getPartySize())
                .build();
        
        String traceId = UUID.randomUUID().toString();
        MessageEnvelope<ReservationCancelledEvent> envelope = MessageEnvelope.<ReservationCancelledEvent>builder()
                .eventType("reservation.cancelled")
                .status("CANCELLED")
                .occurredAt(Instant.now())
                .traceId(traceId)
                .payload(event)
                .build();
        
        eventProducer.sendReservationCancelled(envelope);

        return cancelledReservation;
    }

    public Reservation getReservation(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
    }

    public List<Reservation> getAllReservations() {
        log.info("Retrieving all reservations");
        return reservationRepository.findAll();
    }

    @Transactional
    public void deleteReservation(UUID reservationId) {
        log.info("Deleting reservation with id: {}", reservationId);
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        // If reservation is not cancelled, release seats back to Restaurant Service
        if (reservation.getStatus() != Reservation.ReservationStatus.CANCELLED) {
            log.info("Releasing {} seats for slot {} before deletion", reservation.getPartySize(), reservation.getSlotId());
            try {
                ReleaseRequestDTO releaseRequest = new ReleaseRequestDTO(
                        reservation.getSlotId(),
                        reservation.getPartySize()
                );
                restaurantServiceClient.releaseSeats(releaseRequest);
            } catch (FeignException e) {
                log.error("Failed to release seats during deletion", e);
                throw new IllegalStateException("Failed to release seats during deletion", e);
            }
        }

        reservationRepository.deleteById(reservationId);
        log.info("Reservation {} deleted successfully", reservationId);
    }

    // DTO for Create Reservation Request
    public static class CreateReservationRequest {
        private Long restaurantId;
        private Long slotId;
        private String userEmail;
        private Integer partySize;

        public CreateReservationRequest() {}

        public CreateReservationRequest(Long restaurantId, Long slotId, String userEmail, Integer partySize) {
            this.restaurantId = restaurantId;
            this.slotId = slotId;
            this.userEmail = userEmail;
            this.partySize = partySize;
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public void setRestaurantId(Long restaurantId) {
            this.restaurantId = restaurantId;
        }

        public Long getSlotId() {
            return slotId;
        }

        public void setSlotId(Long slotId) {
            this.slotId = slotId;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }

        public Integer getPartySize() {
            return partySize;
        }

        public void setPartySize(Integer partySize) {
            this.partySize = partySize;
        }
    }
}
