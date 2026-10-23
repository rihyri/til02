package io.github.rihyri.til02.week06.day04.example.exception;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ConcertExceptionHandler {

    // 공연이 존재하지 않거나 좌석이 부족한 경우
    @ExceptionHandler(ConcertException.class)
    public ResponseEntity<Map<String, String>> handleConcertException(ConcertException exception) {

        return ResponseEntity.badRequest()
                .body(Map.of("message", exception.getMessage()));
    }

    // 낙관적 락 충돌이 발생한 경우
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLock(OptimisticLockingFailureException exception) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "message",
                        "다른 사용자가 먼저 예약을 처리했습니다. 다시 시도해 주세요."
                ));
    }
}
