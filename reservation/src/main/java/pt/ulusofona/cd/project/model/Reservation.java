package pt.ulusofona.cd.project.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @NotNull
    @Column(name = "restaurant_id")
    @JsonProperty("restaurant_id")
    private Long restaurantId;

    @NotNull
    @Column(name = "slot_id")
    @JsonProperty("slot_id")
    private Long slotId;

    @NotBlank
    @Email
    @Column(name = "user_email")
    @JsonProperty("user_email")
    private String userEmail;

    @NotNull
    @Column(name = "party_size")
    @JsonProperty("party_size")
    private Integer partySize;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReservationStatus status;

    @NotNull
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = ReservationStatus.PENDING;
        }
    }

    public enum ReservationStatus {
        PENDING, CONFIRMED, CANCELLED
    }
}
