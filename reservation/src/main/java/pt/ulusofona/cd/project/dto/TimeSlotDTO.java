package pt.ulusofona.cd.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDTO {
    private Long id;

    @JsonProperty("restaurantId")
    private Long restaurantId;

    private LocalDate date;

    @JsonProperty("startTime")
    private LocalTime startTime;

    @JsonProperty("endTime")
    private LocalTime endTime;

    private Integer capacity;

    @JsonProperty("seatsAvailable")
    private Integer seatsAvailable;
}
