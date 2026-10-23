package io.github.rihyri.til02.week06.day03.example.service;

import io.github.rihyri.til02.week06.day03.example.domain.Workshop;
import io.github.rihyri.til02.week06.day03.example.dto.ReadComparison;
import io.github.rihyri.til02.week06.day03.example.repository.WorkshopRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkshopIsolationService {

    private final WorkshopRepository workshopRepository;

    // JPA 영속성 컨텍스트를 직접 관리하기 위해 사용한다.
    private final EntityManager entityManager;

    // --------------------------------------------------
    // 1. Dirty Read
    // --------------------------------------------------
    @Transactional
    public void changeAndRollback(
            Long workshopId, CountDownLatch updated, CountDownLatch allowRollback
    ) {
        Workshop workshop = findWorkshop(workshopId);

        // 트랜잭션 A에서 신청 인원을 99명으로 수정한다.
        workshop.changeEnrolledCount(99);

        // 변경 내용을 DB에 저장하지만 COMMIT하지는 않는다.
        // 따라서 다른 트랜잭션에서 읽을 수 있는지는 격리 수준에 따라 달라진다.
        workshopRepository.flush();

        log.info("[A] 신청 인원을 99명으로 변경 (미커밋)");

        // UPDATE가 끝났음을 테스트 스레드에게 알린다.
        updated.countDown();

        // 트랜잭션 B가 조회를 마칠 때까지의 A의 트랜잭션을 유지한다.
        await(allowRollback);

        // RuntimeException이 발생하면 @Transactional에 의해 롤백된다.
        // 따라서 최종 DB에는 99가 아닌 기존 신청 인원이 남는다.
        log.info("[A] 예외 발생으로 롤백");
        throw new IllegalStateException("롤백 실습");
    }

    // 커밋되지 않은 데이터도 읽을 수 있는 가장 낮은 격리 수준
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public int readUncommitted(Long workshopId) {

        // A가 아직 커밋하지 않은 99를 읽을 수 있다.
        // MySQL InnoDB 기준 Dirty Read 재현용 메서드이다.
        int count = findWorkshop(workshopId).getEnrolledCount();

        log.info("[B] READ_UNCOMMITTED 조회: {}", count);
        return count;
    }

    // 커밋된 데이터만 읽도록 제한하는 격리 수준
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int readCommitted(Long workshopId) {

        // A가 수정한 99는 미커밋 상태이므로 읽지 않는다.
        // 기존에 커밋된 신청 인원 20을 초과한다.
        int count = findWorkshop(workshopId).getEnrolledCount();

        log.info("[B] READ_COMMITTED 조회: {}", count);
        return count;
    }

    // --------------------------------------------------
    // 2. Non-repeatable Read
    // --------------------------------------------------
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReadComparison compareWithReadCommitted(
            Long workshopId, CountDownLatch firstRead, CountDownLatch allowSecondRead
    ) {

        // READ_COMMITTED에서는 다른 트랜잭션이 중간에 커밋하면
        // 동일한 다ㅔ이터를 다시 조회했을 때 값이 달라질 수 있다.
        return compareEnrollment(
                workshopId, firstRead, allowSecondRead
        );
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public ReadComparison compareWithRepeatableRead(
            Long workshopId, CountDownLatch firstRead, CountDownLatch allowSecondRead
    ) {

        // MySQL InnoDB의 일반적인 스냅샷 조회에서는
        // 다른 트랜잭션이 수정하더라도 최초 조회 결과를 유지한다.
        return compareEnrollment(
                workshopId, firstRead, allowSecondRead
        );
    }

    // 두 격리 수준에서 동일한 조회 로직을 사용하도록 분리한 메서드
    // 실제 트랜잭션의 격리 수준은 호출한 public 메서드에서 결정된다.
    private ReadComparison compareEnrollment(
            Long workshopId, CountDownLatch firstRead, CountDownLatch allowSecondRead
    ) {

        // 1. 트랜잭션 A에서 최초 데이터 조회
        int first = findWorkshop(workshopId).getEnrolledCount();
        log.info("[A] 첫 번째 조회: {}", false);

        // 2. 첫 번째 조회가 끝났음을 알린다.
        // 테스트에서는 이 신호를 받고 트랜잭션 B를 실행한다.
        firstRead.countDown();

        // 3. B가 데이터를 수정하고 커밋할 때까지 기다린다.
        await(allowSecondRead);

        // 4. JPA 1차 캐시에 남아 있는 엔티티를 제거한다.
        // clear()를 호출하지 않으면 DB 대신 캐시의 기존 값을 읽을 수 있다.
        entityManager.clear();

        // 5. 같은 트랜잭션 A에서 데이터를 다시 조회한다.
        // 격리 수준에 따라 첫 번째 조회와 결과가 달라질 수 있다.
        int second = findWorkshop(workshopId).getEnrolledCount();
        log.info("[A] 두 번째 조회: {}", second);

        return new ReadComparison(first, second);
    }

    @Transactional
    public void updateEnrollment(Long workshopId, int count) {

        Workshop workshop = findWorkshop(workshopId);

        // 트랜잭션 B에서 신청 인원을 수정한다.
        // 영속 상태의 엔티티이므로 변경 감지가 적용된다.
        workshop.changeEnrolledCount(count);

        log.info("[B] 신청 인원을 {}명으로 변경", count);

        // 메서드가 정상 종료되면 트랜잭션이 커밋된다.
        // 이후 A의 두 번째 조회 결과를 비교한다.
    }

    // --------------------------------------------------
    // 3. Phantom Read
    // --------------------------------------------------
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReadComparison compareRangeWithReadCommitted(CountDownLatch firstRead, CountDownLatch allowSecondRead) {

        // 다른 트랜잭션이 새로운 행을 INSERT하고 커밋하면
        // 두 번째 조회에서 조건에 맞는 행이 증가할 수 있다.
        return compareWorkshopCount(firstRead, allowSecondRead);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public ReadComparison compareRangeWithRepeatableRead(CountDownLatch firstRead, CountDownLatch allowSecondRead) {

        // MySQL InnoDB의 일반 SELECT에서는 일관된 스냅샷을 사용한다.
        // 따라서 새로운 행이 커밋되어도 잉전 조회 결과가 유지된다.
        return compareWorkshopCount(firstRead, allowSecondRead);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReadComparison compareRangeWithSerializable(CountDownLatch firstRead, CountDownLatch allowSecondRead) {

        // 더 강한 격리 수준으로 동시 작업 간 충돌을 제한한다.
        // MySQL InnoDB에서는 조회에 공유 잠금이 적용되어 다른 트랜잭션의 INSERT가 대기할 수 있다.
        return compareWorkshopCount(firstRead, allowSecondRead);
    }

    // 특정 조건에 맞는 강의의 개수를 두 번 조회하는 공통 메서드
    private ReadComparison compareWorkshopCount(CountDownLatch firstRead, CountDownLatch allowSecondRead) {

        // 1. 신청 인원이 10명보다 많은 강의 개수를 조회한다.
        long first = workshopRepository.countByEnrolledCountGreaterThan(10);

        log.info("[A] 첫 번째 강의 개수: {}", first);

        // 2. 첫 번째 조회 완료를 알린다.
        firstRead.countDown();

        // 3. 다른 트랜잭션이 새로운 강의를 INSERT하도록 기다린다.
        await(allowSecondRead);

        // 4. 동일한 조건으로 다시 조회한다.
        // 새로운 행이 조회되는지 확인하는 것이 핵심이다.
        long second = workshopRepository.countByEnrolledCountGreaterThan(10);

        log.info("[A] 두 번째 강의 개수: {}", second);

        return new ReadComparison(first, second);
    }

    @Transactional
    public void addWorkshop(String title, int count) {

        Workshop workshop = Workshop.builder()
                .title(title)
                .enrolledCount(count)
                .build();

        // Phantom Read 실습을 위해 조건에 맞는 새로운 행을 추가한다.
        // flush()는 INSERT SQL을 전달하며 실제 커밋은 트랜잭션 종료 시 발생한다.
        workshopRepository.saveAndFlush(workshop);

        log.info("[B] 새로운 강의 등록: {}", title);
    }

    // --------------------------------------------------
    // 공통 메서드
    // --------------------------------------------------
    private Workshop findWorkshop(Long workshopId) {

        return workshopRepository.findById(workshopId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 강의입니다."
                        ));
    }

    // 두 스레드의 실행 순서를 맞추기 위핸 대기 메서드
    private void await(CountDownLatch latch) {

        try {
            // countDown()이 호출되면 대기가 해제된다.
            // 무한 대기를 방지하기 위해 최대 10초까지만 기다린다.
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("스레드 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException e) {
            // 인터럽트 상태를 복원하고 예외를 전달한다.
            Thread.currentThread().interrupt();

            throw new IllegalStateException("스레드 대기 중 인터럽트가 발생했습니다.", e);
        }
    }
}
