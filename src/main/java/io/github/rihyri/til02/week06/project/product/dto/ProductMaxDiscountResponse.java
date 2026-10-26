package io.github.rihyri.til02.week06.project.product.dto;

import io.github.rihyri.til02.week06.project.coupon.entity.Coupon;
import io.github.rihyri.til02.week06.project.coupon.entity.CouponUser;
import io.github.rihyri.til02.week06.project.coupon.entity.DiscountType;
import io.github.rihyri.til02.week06.project.product.entity.Product;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductMaxDiscountResponse {

    Long productId;
    String productName;
    BigDecimal originalPrice;
    BigDecimal discountAmount;
    BigDecimal discountedPrice;
    BigDecimal discountRate;

    // 적용 가능한 쿠폰이 없을 경우 null
    AppliedCoupon appliedCoupon;

    @Getter
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AppliedCoupon {

        Long couponId;
        Long couponUserId;
        String couponName;
        DiscountType discountType;
        Integer discountValue;
        Integer maxDiscountAmount;
    }

    // 가장 높은 할인 금액을 제공하는 쿠폰을 응답 DTO로 변환한다.
    public static ProductMaxDiscountResponse of(
            Product product, CouponUser couponUser, BigDecimal discount
    ) {
        Coupon coupon = couponUser.getCoupon();
        BigDecimal price = product.getPrice();

        return ProductMaxDiscountResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .originalPrice(price)
                .discountAmount(discount)
                .discountedPrice(price.subtract(discount))
                .discountRate(rate(discount, price))
                .appliedCoupon(
                        AppliedCoupon.builder()
                                .couponId(coupon.getId())
                                .couponUserId(couponUser.getId())
                                .couponName(coupon.getCouponName())
                                .discountType(coupon.getDiscountType())
                                .discountValue(coupon.getDiscountValue())
                                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                                .build()
                )
                .build();
    }

    // 적용 가능한 쿠폰이 없다면 원래 상품 가격을 반환한다.
    public static ProductMaxDiscountResponse noCoupon(Product product) {

        return ProductMaxDiscountResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .originalPrice(product.getPrice())
                .discountAmount(BigDecimal.ZERO)
                .discountedPrice(product.getPrice())
                .discountRate(BigDecimal.ZERO)
                .build();
    }

    // 실제 할인 금액을 기준으로 할인율을 계산한다.
    private static BigDecimal rate(BigDecimal discount, BigDecimal price) {

        // 0으로 나누는 상황을 방지한다.
        if (price.signum() == 0) {
            return BigDecimal.ZERO;
        }

        return discount
                .multiply(BigDecimal.valueOf(100))
                .divide(price, 2, RoundingMode.HALF_UP);
    }
}
