package io.github.rihyri.til02.week06.project.coupon.dto.response;

import io.github.rihyri.til02.week06.project.coupon.entity.Coupon;
import io.github.rihyri.til02.week06.project.coupon.entity.CouponUser;
import io.github.rihyri.til02.week06.project.coupon.entity.CouponUserStatus;
import io.github.rihyri.til02.week06.project.coupon.entity.DiscountType;
import io.github.rihyri.til02.week06.project.coupon.repository.CouponUserRepository;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponUserResponse {

    Long coupon_user_id;
    String code;
    CouponUserStatus status;
    LocalDateTime registeredAt;

    Long coupon_id;
    String coupon_name;
    DiscountType discount_type;
    Integer discount_value;
    Integer minOrderAmount;
    Integer maxDiscountAmount;
    LocalDateTime startDate;
    LocalDateTime endDate;

    // 사용자 쿠폰 정보와 연결된 쿠폰 정책을 함께 반환한다.
    public static CouponUserResponse from(CouponUser couponUser) {

        Coupon coupon = couponUser.getCoupon();

        return CouponUserResponse.builder()
                .coupon_user_id(couponUser.getId())
                .code(couponUser.getCode())
                .status(couponUser.getStatus())
                .registeredAt(couponUser.getRegisteredAt())
                .coupon_id(coupon.getId())
                .coupon_name(coupon.getCouponName())
                .discount_type(coupon.getDiscountType())
                .discount_value(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .build();
    }
}
