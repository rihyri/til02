package io.github.rihyri.til02.week06.project.coupon.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String couponName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    // 정률 할인은 퍼센트, 정액 할인은 원 단위
    @Column(nullable = false)
    private Integer discountValue;

    @Column(nullable = false)
    private Integer minOrderAmount;

    // null이면 최대 할인 금액 제한 없음
    private Integer maxDiscountAmount;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    // null이면 발행 수량 제한 없음
    private Integer usageLimit;

    @Column(nullable = false)
    private Integer issueCount = 0;

    @Column(nullable = false)
    private Integer usedCount = 0;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Builder
    private Coupon(
            String couponName,
            DiscountType discountType,
            Integer discountValue,
            Integer minOrderAmount,
            Integer maxDiscountAmount,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer usageLimit
    ) {
        this.couponName = couponName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usageLimit = usageLimit;
    }

    /*
     *  실제 데이터를 삭제하지 않고 삭제 상태로 변경한다.
     *  트랜잭션 안에서 조회한 영속성 엔티티이므로 변경 감지를 통해 UPDATE가 실행된다.
     */
    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    // 상품 가격이 쿠폰의 최소 주문 금액인지 확인
    public boolean meetsMinOrderAmount(BigDecimal price) {
        return price.compareTo(
                BigDecimal.valueOf(minOrderAmount)
        ) >= 0;
    }

    // 쿠폰 정책에 따라 실제 할인 금액을 계산한다.
    public BigDecimal calculateDiscount(BigDecimal price) {

        BigDecimal discount = switch (discountType) {
            case FIXED -> BigDecimal.valueOf(discountValue);

            case PERCENTAGE -> {
                // 정률 할인 계산 후 소수점 이하는 버린다.
                BigDecimal raw = price
                        .multiply(BigDecimal.valueOf(discountValue))
                        .divide(
                                BigDecimal.valueOf(100),
                                0,
                                RoundingMode.DOWN
                        );

                // 최대 할인 금액이 존재한다면 상한을 적용한다.
                yield maxDiscountAmount == null ? raw : raw.min(BigDecimal.valueOf(maxDiscountAmount));
            }
        };

        // 상품 가격보다 할인 금액이 커지지 않도록 제한
        return discount.min(price);
    }

    // 쿠폰이 삭제되지 않았고 종료일이 지나지 않았는지 확인
    public boolean isRegisterable(LocalDateTime now) {
        return !isDeleted && !now.isAfter(endDate);
    }
}
