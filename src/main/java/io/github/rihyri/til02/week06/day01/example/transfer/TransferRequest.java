package io.github.rihyri.til02.week06.day01.example.transfer;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @Min(1)
    private Long fromId;

    @Min(1)
    private Long toId;

    @Min(1)
    private long amount;

    private boolean failAfterWithdrawal;
}
