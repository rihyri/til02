package io.github.rihyri.til02.week06.day04.example.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReservationRequest {

    @NotNull
    @Min(1)
    private Integer quantity;
}
