package io.github.rihyri.til02.week06.day04.example.service;

import io.github.rihyri.til02.week06.day04.example.domain.Concert;
import io.github.rihyri.til02.week06.day04.example.dto.ConcertResponse;
import io.github.rihyri.til02.week06.day04.example.exception.ConcertException;
import io.github.rihyri.til02.week06.day04.example.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PessimisticReservationService {

    private final ConcertRepository concertRepository;

    @Transactional
    public void reserve(Long concertId, int quantity) {

        // SELECT ... FOR UPDATE 실행
        // 다른 트랜잭션이 동일한 행을 잠갔다면 락 해제까지 대기한다.
        Concert concert = concertRepository.findByIdWithLock(concertId)
                .orElseThrow(() -> new ConcertException("공연을 찾을 수 없습니다."));

        // 락을 획득한 상태에서 잔여 좌석을 검증하고 수정한다.
        if (concert.getRemainingSeats() < quantity) {
            throw new ConcertException("예약 가능한 좌석이 부족합니다.");
        }

        concert.reserve(quantity);

        // 영속 상태의 Entity이므로 변경 감지로 UPDATE가 실행된다.
        // 트랜잭션이 커밋되면 획득한 락이 해제된다.
    }

    @Transactional(readOnly = true)
    public ConcertResponse getConcert(Long concertId) {

        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ConcertException("공연을 찾을 수 없습니다."));

        return ConcertResponse.from(concert);
    }
}
