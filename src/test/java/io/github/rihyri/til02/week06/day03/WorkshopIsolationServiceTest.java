package io.github.rihyri.til02.week06.day03;

import io.github.rihyri.til02.week06.day03.example.domain.Workshop;
import io.github.rihyri.til02.week06.day03.example.dto.ReadComparison;
import io.github.rihyri.til02.week06.day03.example.repository.WorkshopRepository;
import io.github.rihyri.til02.week06.day03.example.service.WorkshopIsolationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.Reader;
import java.util.concurrent.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Week6Day03Application.class)
@ActiveProfiles("day03")
public class WorkshopIsolationServiceTest {

    @Autowired
    private WorkshopIsolationService isolationService;

    @Autowired
    private WorkshopRepository workshopRepository;

    private ExecutorService executor;
    private Long workshopId;

    @BeforeEach
    void setUp() {

        // 두 개의 스레드를 사용해 독립적인 트랜잭션을 실행한다.
        executor = Executors.newFixedThreadPool(2);

        // 각 테스트가 동일한 초기 상태에서 시작하도록 데이터를 초기화한다.
        workshopRepository.deleteAll();

        Workshop workshop = Workshop.builder()
                .title("Spring 기본 과정")
                .enrolledCount(20)
                .build();

        // INSERT SQL을 실행하고 실습에 사용할 강의 ID를 저장한다.
        workshopId = workshopRepository
                .saveAndFlush(workshop)
                .getId();
    }

    @AfterEach
    void tearDown() {

        // 테스트에서 사용한 스레드 풀을 종료한다.
        executor.shutdownNow();
    }


    // --------------------------------------------------
    // 1. Dirty Read
    // --------------------------------------------------
    @Test
    @DisplayName("READ_UNCOMMITTEDDPT에서는 미커밋 데이터를 읽는다.")
    void dirtyReadOccurs() throws Exception {

        // A가 변경했지만 커밋하지 않은 값을 B가 조회한다.
        int count = runDirtyRead(
            isolationService::readUncommitted
        );

        // 아직 커밋되지 않은 99를 읽는다.
        assertEquals(99, count);

        // A가 롤백했으므로 DB에는 원래 값이 남는다.
        assertEquals(20, getCurrentCount());
    }

    @Test
    @DisplayName("READ_COMMITTED에서는 Dirty Read를 방지한다")
    void dirtyReadPrevented() throws Exception {

        // 이번에는 커밋된 데이터만 읽도록 격리 수준을 변경한다.
        int count = runDirtyRead(
                isolationService::readCommitted
        );

        // A가 변경한 99는 아직 커밋되지 않았으므로 읽지 않는다.
        assertEquals(20, count);
        assertEquals(20, getCurrentCount());
    }

    // Dirty Read 테스트에서 사용하는 공통 실행 로직
    private int runDirtyRead(Function <Long, Integer> reader) throws Exception {

        // A의 UPDATE 완료 여부를 확인하기 위한 신호
        CountDownLatch updated = new CountDownLatch(1);

        // B의 조회가 끝날 때까지 A의 롤백을 지연시키는 신호
        CountDownLatch allowRollback = new CountDownLatch(1);

        // 별도 스레드에서 트랜잭션 A를 실행한다.
        Future<?> write = executor.submit(() ->
                isolationService.changeAndRollback(
                        workshopId, updated, allowRollback
                )
        );

        try {
            // A가 신청 인원을 99로 수정할 때까지 기다린다.
            assertTrue(updated.await(10, TimeUnit.SECONDS));

            // A의 트랜잭션이 아직 커밋되지 않은 상태에서 B가 조회된다.
            // reader에 전달한 메서드에 따라 격리 수준이 달라진다.
            return reader.apply(workshopId);
        } finally {
            // B의 조회가 끝났으므로 A의 대기를 해제한다.
            allowRollback.countDown();

            // A는 의도적으로 예외를 발생시키므로 Futer.get()에서도 예외가 전달된다.
            ExecutionException exception =
                    assertThrows(
                            ExecutionException.class, () -> write.get(10, TimeUnit.SECONDS)
                    );

            // 의도한 롤백 예외가 발생했는지 확인한다.
            assertInstanceOf(IllegalStateException.class, exception.getCause());
        }
    }

    // --------------------------------------------------
    // 2. Non-repeatable Read
    // --------------------------------------------------
    @Test
    @DisplayName("READ_COMMITTED에서는 두 조회 결과가 달라진다.")
    void nonRepeatableRealOccurs() throws Exception {

        // A가 첫 조회를 마친 뒤 B가 값을 수정하고 커밋한다.
        ReadComparison result = runUpdateBetweenReads(isolationService::compareWithReadCommitted);

        // A가 최초에 조회한 값은 20이다.
        assertEquals(20, result.getFirstValue());

        // B의 커밋 이후 다시 조회하면 7로 변경되어 있다.
        assertEquals(7, result.getSecondValue());

        // 같은 트랜잭션 안에서 조회 결과가 달라졌다.
        assertTrue(result.isChanged());
    }

    @Test
    @DisplayName("REPEATABLE_READ에서는 동일한 값을 받는다.")
    void nonRepeatableReadPrevent() throws Exception {

        ReadComparison result = runUpdateBetweenReads(isolationService::compareWithRepeatableRead);

        // A에서는 동일한 스냅샷을 사용하므로 두 조회 결과가 같다.
        assertEquals(20, result.getFirstValue());
        assertEquals(20, result.getSecondValue());

        // 하지만 B의 변경 자체가 취소된 것은 아니다.
        // A의 트랜잭션이 종료된 후 조회하면 최종 값은 7이다.
        assertEquals(7, getCurrentCount());
    }

    // A의 두 조회 사이에 B가 UPDATE를 수행하도록 제어한다.
    private ReadComparison runUpdateBetweenReads(EnrollmentReader reader) throws Exception {

        CountDownLatch firstRead = new CountDownLatch(1);
        CountDownLatch allowSecondRead = new CountDownLatch(1);

        // 트랜잭션 A를 별도로 스레드에서 실행한다.
        Future<ReadComparison> reading = executor.submit(() ->
                reader.read(
                        workshopId, firstRead, allowSecondRead
                )
        );

        try {
            // 1. A가 첫 번째 조회를 완료할 때까지 대기
            assertTrue(firstRead.await(10, TimeUnit.SECONDS));

            // 2. B가 신청 인원을 20에서 7로 변경하고 커밋
            // 별도 @Transactional 메서드이므로 독립적인 트랜잭션으로 실행된다.
            isolationService.updateEnrollment(workshopId, 7);
        } finally {

            // 3. B의 작업이 끝났으므로 A의 두 번째 조회를 허용
            allowSecondRead.countDown();
        }

        // 4. A가 두 번 조횐 결과를 받아 비교한다.
        return reading.get(10, TimeUnit.SECONDS);
    }

    // --------------------------------------------------
    // 3. Phantom Read
    // --------------------------------------------------
    @Test
    @DisplayName("READ_COMMITTED에서는 조회 대상 행이 증가한다.")
    void phantomReadOccurs() throws Exception {

        // A의 두 범위 조회 사이에 B가 새로운 강의를 INSERT한다.
        ReadComparison result = runInsertBetweenReads(
                isolationService::compareRangeWithReadCommitted
        );

        // 최초에는 신청 인원이 10명보다 많은 강의가 1개이다ㅏ.
        assertEquals(1, result.getFirstValue());

        // B가 새 강의를 추가했으므로 두 번째 조회 결과는 2개가 된다.
        assertEquals(2, result.getSecondValue());
    }

    @Test
    @DisplayName("InnoDB REPEATABLE_READ에서는 스냅샷이 유지된다.")
    void phantomReadPreventedBySnapshot() throws Exception {

        ReadComparison result = runInsertBetweenReads(
                isolationService::compareRangeWithRepeatableRead
        );

        // MySQL InnoDB의 일반 SELECT는 동일한 스냅샷을 사용한다.
        // 따라서 중간 INSERT가 발생해도 조회 결과는 1개로 유지된다.
        assertEquals(1, result.getFirstValue());
        assertEquals(1, result.getSecondValue());

        // A의 트랜잭션이 종료된 뒤에는 새롭게 추가된 강의가 조회된다.
        assertEquals(
                2, workshopRepository.countByEnrolledCountGreaterThan(10)
        );
    }

    // 동일한 조회 조건 사이에 새로운 행을 INSERT하는 공통 로직
    private ReadComparison runInsertBetweenReads(RangeReader reader
    ) throws Exception {

        CountDownLatch firstRead = new CountDownLatch(1);
        CountDownLatch allowSecondRead = new CountDownLatch(1);

        // 트랜잭션 A에서 조건에 맞는 강의 개수를 조회한다.
        Future<ReadComparison> reading = executor.submit(() ->
                reader.read(firstRead, allowSecondRead)
        );

        try {
            // 1. A가 첫 번째 조회를 완료할 때까지 기다린다.
            assertTrue(firstRead.await(10, TimeUnit.SECONDS));

            // 2. B에서 조건에 해당하는 새로운 강의를 추가한다.
            // 신청 인원이 15명이므로 enrolledCount > 10 조건을 만족한다.
            isolationService.addWorkshop(
                    "Spring 심화 과정", 15
            );

        } finally {

            // 3. INSERT가 끝난 뒤 A의 두 번째 조회를 허용한다.
            allowSecondRead.countDown();
        }

        // 4. 두 번 조회한 강의 개수를 반환한다.
        return reading.get(10, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("SERIALIZABLE에서는 범위 조회 중 INSERT가 제한된다.")
    void phantomReadPreventBySerializable() throws Exception {

        CountDownLatch firstRead = new CountDownLatch(1);
        CountDownLatch allowSecondRead = new CountDownLatch(1);

        // 트랜잭션 A를 SERIALIZABLE 격리 수준으로 실행한다.
        Future<ReadComparison> reading = executor.submit(() ->
            isolationService.compareRangeWithSerializable(
                    firstRead, allowSecondRead
            )
        );

        try {
            // 1. A가 첫 번째 범위 조회를 완료할 때까지 대기한다.
            assertTrue(firstRead.await(10, TimeUnit.SECONDS));

            // 2. A의 트랜잭션이 유지되는 동안 B가 INSERT를 시도한다.
            // DB의 잠금 상태에 따라 B의 작업이 대기할 수 있다.
            Future<?> inserting = executor.submit(() ->
                    isolationService.addWorkshop(
                            "JPA 심화 과정", 15
                    )
            );

            try {
                // INSERT 작업을 별도 스레드로 실행한 상태이다.
                // 아직 INSERT가 실제 잠금 대기에 진입했는지는 확인하지 않는다.
            } finally {

                // 3. A가 두 번째 조회를 마치고 종료할 수 있도록 허용한다.
                allowSecondRead.countDown();
            }

            // 4. A의 두 범위 조회 결과를 확인한다.
            ReadComparison result = reading.get(10, TimeUnit.SECONDS);

            assertEquals(1, result.getFirstValue());
            assertEquals(2, result.getSecondValue());

            // 5. A의 트랜잭션이 종료되면 B의 INSERT가 완료될 수 있다.
            inserting.get(10, TimeUnit.SECONDS);

            // 새로운 강의가 정상적으로 추가됐는지 확인한다.
            assertEquals(
                    2, workshopRepository.countByEnrolledCountGreaterThan(10)
            );
        } finally {
            allowSecondRead.countDown();
        }
    }

    // --------------------------------------------------
    // 테스트 보조 코드
    // --------------------------------------------------
    private int getCurrentCount() {

        // 새로운 조회에 실제로 커밋된 신청 인원을 확인한다.
        return workshopRepository.findById(workshopId)
                .orElseThrow()
                .getEnrolledCount();
    }

    @FunctionalInterface
    private interface EnrollmentReader {

        // 격리 수준별 조회 메서드를 공통 테스트 로직에 전달한다.
        ReadComparison read(
                Long id, CountDownLatch first, CountDownLatch next
        );
    }

    @FunctionalInterface
    private interface RangeReader {

        // Phantom Read 실습에서 사용할 범위 조회 메서드를 전달한다.
        ReadComparison read(
                CountDownLatch first, CountDownLatch next
        );
    }
}
