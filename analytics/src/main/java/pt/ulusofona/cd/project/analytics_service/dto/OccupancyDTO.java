package pt.ulusofona.cd.project.analytics_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupancyDTO {

    @JsonProperty("date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonProperty("total_guests")
    private Long totalGuests;

    @JsonProperty("total_reservations")
    private Long totalReservations;

    @JsonProperty("average_party_size")
    private Double averagePartySize;
}
