package io.github.rihyri.til02.week06.day04;

import io.github.rihyri.til02.week06.day04.example.domain.Concert;
import io.github.rihyri.til02.week06.day04.example.repository.ConcertRepository;
import io.github.rihyri.til02.week06.day04.example.service.OptimisticReservationService;
import io.github.rihyri.til02.week06.day04.example.service.PessimisticReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ReservationConcurrencyTest {

    private static final long CONCERT_ID = 1L;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private PessimisticReservationService pessimisticService;

    @Autowired
    private OptimisticReservationService optimisticService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {

        // 각 테스트 전에 DB 상태를 동일하게 초기화한다.
        // @Transactional을 사용하지 않아 각 작업이 별도 트랜잭션에서 수행된다.
        jdbcTemplate.update(
                """
                    UPDATE concerts
                    SET remaining_seats = 10,
                        version = 0
                    WHERE id = ?    
                    """,
                    CONCERT_ID
        );
    }

    @Test
    @DisplayName("비관적 락으로 동시 예약을 처리한다")
    void reserveWithPessimisticLock() throws Exception {

        // 서로 다른 스레드에서 예약을 동시에 요청한다.
        int successCount = executeConcurrently(
                () -> pessimisticService.reserve(CONCERT_ID, 7)
        );

        Concert concert = concertRepository.findById(CONCERT_ID).orElseThrow();

        // 한 요청만 성공하고, 나머지 요청은 좌석 부족으로 실패해야 한다.
        assertThat(successCount).isEqualTo(1);
        assertThat(concert.getRemainingSeats()).isEqualTo(3);
    }

    @Test
    @DisplayName("낙관적 락으로 동시 예약을 처리한다.")
    void reserveWithOptimisticLock() throws Exception {

        int successCount = executeConcurrently(
                () -> optimisticService.reserve(CONCERT_ID, 7)
        );

        Concert concert = concertRepository.findById(CONCERT_ID)
                .orElseThrow();

        // 낙관적 락 충돌 또는 좌석 부족으로 한 요청만 성공한다.
        assertThat(successCount).isEqualTo(1);
        assertThat(concert.getRemainingSeats()).isEqualTo(3);
    }

    private int executeConcurrently(Runnable reservationTask)
            throws Exception {

        int threadCount = 2;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 두 스레드의 작업 시작 시점을 최대한 맞춘다.
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {

                futures.add(executor.submit(() -> {

                    ready.countDown();

                    // 두 작업을 모두 준비시킨 뒤 실행한다.
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("시작 대기 시간 초과");
                    }

                    try {
                        reservationTask.run();
                        return true;

                    } catch (RuntimeException exception) {

                        // 락 충돌 또는 좌석 부족으로 실패한 요청
                        System.out.println(
                                "예약 실패: " + exception.getMessage()
                        );

                        return false;
                    }
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            // 준비된 스레드들이 예약 로직을 시작하도록 허용한다.
            start.countDown();

            int successCount = 0;

            for (Future<Boolean> future : futures) {
                if (future.get(10, TimeUnit.SECONDS)) {
                    successCount++;
                }
            }

            return successCount;

        } finally {
            executor.shutdownNow();
        }
    }
}
