package io.github.rihyri.til02.week06.project.coupon.repository;

import io.github.rihyri.til02.week06.project.coupon.entity.CouponUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CouponUserRepository extends JpaRepository<CouponUser, Long> {

    // 사용자가 보유한 쿠폰 중 실제 할인에 사용할 수 있는 쿠폰 조회
    @Query("""
            SELECT cu FROM CouponUser cu
            JOIN FETCH cu.coupon c
            WHERE cu.user.id = :userId
                AND cu.usedAt IS NULL
                AND c.isDeleted = false
                AND c.startDate <= :now
                AND c.endDate >= :now
                AND (
                    c.usageLimit IS NULL
                    OR c.usedCount < c.usageLimit
                )
            """)
    List<CouponUser> findUsableCoupons(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /*
     *  비관적 쓰기 락으로 특정 쿠폰 코드를 잠근다.
     *  동일한 코드에 대한 다른 트랜잭션의 쓰기 락 획득은 현재 트랜잭션이 종료될 때까지 대기하게 된다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT cu FROM CouponUser cu 
            WHERE cu.code = :code
            """)
    Optional<CouponUser> findByCodeForUpdate(@Param("code") String code);

    /*
     *  새로 생성한 코드가 이미 DB에 존재하는지 확인한다.
     * 단, 조회 시점 이후 다른 트랜잭션이 삽입할 가능성은 남아 있다.
     */
    @Query("""
            SELECT cu.code FROM CouponUser cu
            WHERE cu.code IN :codes
            """)
    List<String> findExistingCodes(@Param("codes") Collection<String> codes);
}
