package io.github.rihyri.til02.week06.day04.example.repository;

import io.github.rihyri.til02.week06.day04.example.domain.Concert;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    // 조회할 때 배타적 락을 획득한다.
    // 동일한 행을 수졍하려는 다른 트랜잭션은 락이 해제될 때까지 대기한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CONERT c WHERE c.id = :concertId")
    Optional<Concert> findByIdWithLock(@Param("concertId") Long concertId);

    // 낙관적 락을 사용하는 조회
    // 별도의 DB 배타적 락을 획득하지 않고 버전을 확인한다.
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM Concert c WHERE c.id = :concertId")
    Optional<Concert> findByIdWithVersion(@Param("concertId") Long concertId);
}
