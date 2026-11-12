package io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/*
 *  Value Object : 정률(%) 할인 쿠폰
 *  BDD 시나리오 "할인 쿠폰 적용" (1,000원 상품 + 30% 할인 쿠폰 → 700원)을 코드로 옮긴 예제이다.
 *  할인 결과는 원 단위 미만을 버린다.
 */
public record DiscountCoupon(int ratePercent) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public DiscountCoupon {
        if (ratePercent < 1 || ratePercent > 100) {
            throw new IllegalArgumentException("할인율은 1~100% 사이여야 합니다: " + ratePercent);
        }
    }

    public Money apply(Money price) {
        BigDecimal discounted = price.amount()
                .multiply(BigDecimal.valueOf(100L - ratePercent))
                .divide(HUNDRED, 0, RoundingMode.DOWN);
        return new Money(discounted);
    }
}