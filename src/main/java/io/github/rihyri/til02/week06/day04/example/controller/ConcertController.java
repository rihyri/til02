package io.github.rihyri.til02.week06.day04.example.controller;

import io.github.rihyri.til02.week06.day04.example.dto.ConcertResponse;
import io.github.rihyri.til02.week06.day04.example.dto.ReservationRequest;
import io.github.rihyri.til02.week06.day04.example.service.OptimisticReservationService;
import io.github.rihyri.til02.week06.day04.example.service.PessimisticReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final PessimisticReservationService pessimisticService;
    private final OptimisticReservationService optimisticService;

    // 현재 공연의 잔여 좌석 및 버전 조회
    @GetMapping("/{concertId}")
    public ConcertResponse getConcert(@PathVariable Long concertId) {

        return pessimisticService.getConcert(concertId);
    }

    // 비관적 락으로 예약 처리
    @PostMapping("/{concertId}/reserve/pessimistic")
    public ResponseEntity<Void> reserveWithPessimisticLock(
            @PathVariable Long concertId,
            @Valid @RequestBody ReservationRequest request
    ) {

        pessimisticService.reserve(concertId, request.getQuantity());
        return ResponseEntity.noContent().build();
    }

    // 낙관적 락으로 예약 처리
    @PostMapping("/{concertId}/reserve/optimistic")
    public ResponseEntity<Void> reserveWithOptimisticLock(
            @PathVariable Long concertId,
            @Valid @RequestBody ReservationRequest request
    ) {

        optimisticService.reserve(concertId, request.getQuantity());
        return ResponseEntity.noContent().build();
    }
}
