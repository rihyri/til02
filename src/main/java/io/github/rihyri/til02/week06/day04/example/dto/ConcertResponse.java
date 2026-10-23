package io.github.rihyri.til02.week06.day04.example.dto;

import io.github.rihyri.til02.week06.day04.example.domain.Concert;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConcertResponse {

    private Long id;
    private String title;
    private Integer remainingSeats;
    private Long version;

    // Entity를 응답DTO로 변환한다.
    public static ConcertResponse from(Concert concert) {
        return new ConcertResponse(
                concert.getId(),
                concert.getTitle(),
                concert.getRemainingSeats(),
                concert.getVersion()
        );
    }
}
