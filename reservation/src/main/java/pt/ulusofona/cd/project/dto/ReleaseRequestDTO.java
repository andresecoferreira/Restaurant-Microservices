package pt.ulusofona.cd.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseRequestDTO {
    @JsonProperty("slotId")
    private Long slotId;

    @JsonProperty("seatCount")
    private Integer seatCount;
}
