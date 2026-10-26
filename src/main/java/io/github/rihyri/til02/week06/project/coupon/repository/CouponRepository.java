package io.github.rihyri.til02.week06.project.coupon.repository;

import io.github.rihyri.til02.week06.project.coupon.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByIdAndIsDeletedFalse(Long id);

    // 삭제된 쿠폰을 제외하고 활성화 여부에 따라 조회한다.
    // isActive가 null이면 활성 상태에 관계없이 조회한다.
    @Query("""
            SELECT c FROM Coupon c
            WHERE c.isDeleted = false AND (
                :isActive IS NULL
                OR (
                    is:Active = true AND c.startDate <= :now AND c.endDate >= :now
                )
                OR (
                    :isActive = false AND ( c.startDate > :now OR c.endDate <: now )
                )
            )
            """)
    Page<Coupon> searchCoupons (
            @Param("isActive") Boolean isActive, @Param("now")LocalDateTime now, Pageable pageable
    );

    // 쿠폰 발행 수량을 DB에서 직접 증가시킨다.
    // 사용 한도를 초과하지 않는 경우에만 UPDATE가 실행된다.
    @Modifying
    @Query("""
            UPDATE Coupon c
            SET c.issueCount = c.issueCount + :count
            WHERE c.id = :couponId
                AND c.isDeleted = false
                AND (
                    c.usageLimit IS NULL
                    OR c.issueCount + :count <= c.usageLimit
                )
            """)
    int increaseIssueCount(@Param("couponId") Long couponId, @Param("count") int count);
}
