package io.github.rihyri.til02.week06.project.coupon.dto.response;

import io.github.rihyri.til02.week06.project.coupon.entity.Coupon;
import io.github.rihyri.til02.week06.project.coupon.entity.DiscountType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponResponse {

    private Long id;
    private String couponName;
    private DiscountType discountType;
    private Integer discountValue;
    private Integer minOrderAmount;
    private Integer maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer issueCount;
    private Integer usedCount;
    private LocalDateTime createdAt;

    @Builder
    private CouponResponse(
            Long id,
            String couponName,
            DiscountType discountType,
            Integer discountValue,
            Integer minOrderAmount,
            Integer maxDiscountAmount,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer usageLimit,
            Integer issueCount,
            Integer usedCount,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.couponName = couponName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usageLimit = usageLimit;
        this.issueCount = issueCount;
        this.usedCount = usedCount;
        this.createdAt = createdAt;
    }

    // Entity를 직접 반환하지 않고 필요한 필드만 DTO로 변환한다.
    public static CouponResponse from(Coupon coupon) {

        return CouponResponse.builder()
                .id(coupon.getId())
                .couponName(coupon.getCouponName())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .usageLimit(coupon.getUsageLimit())
                .issueCount(coupon.getIssueCount())
                .usedCount(coupon.getUsedCount())
                .createdAt(coupon.getCreatedAt())
                .build();
    }
}
