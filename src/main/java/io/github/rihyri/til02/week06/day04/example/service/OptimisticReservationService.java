package io.github.rihyri.til02.week06.day04.example.service;

import io.github.rihyri.til02.week06.day04.example.domain.Concert;
import io.github.rihyri.til02.week06.day04.example.exception.ConcertException;
import io.github.rihyri.til02.week06.day04.example.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OptimisticReservationService {

    private final ConcertRepository concertRepository;

    @Transactional
    public void reserve(Long concertId, int quantity) {

        // 현재 버전을 기준으로 Entity를 조회한다.
        // 비관적 락처럼 미리 배타적 락을 획득하지 않는다.
        Concert concert = concertRepository.findByIdWithVersion(concertId)
                .orElseThrow(() -> new ConcertException("공연을 찾을 수 없습니다."));

        if (concert.getRemainingSeats() < quantity) {
            throw new ConcertException("예약 가능한 좌석이 부족합니다.");
        }

        concert.reserve(quantity);

        // 커밋 과정에서 변경 감지 및 버전 검사가 수행된다.
        // 다른 트랜잭션이 먼저 수행했다면 낙관적 락 충돌이 발생한다.
    }
}
