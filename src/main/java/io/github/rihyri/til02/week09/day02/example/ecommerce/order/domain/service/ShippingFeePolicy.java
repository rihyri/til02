package io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.service;

import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model.Money;

/*
 *  도메인 서비스: 특정 엔티티 하나에 넣기 어색한 비즈니스 규칙을 담는 무상태 객체
 *  "배송비 정책"은 주문의 속성이 아니라 회사의 정책이며, 정책이 바뀌어도 Order 코드는 바뀌지 않는다.
 */
public class ShippingFeePolicy {

    private static final Money FREE_SHIPPING_THRESHOLD = Money.won(30_000);
    private static final Money DEFAULT_FEE = Money.won(3_000);

    public Money calculate(Money linesAmount) {
        return linesAmount.isGreaterThanOrEqual(FREE_SHIPPING_THRESHOLD) ? Money.ZERO : DEFAULT_FEE;
    }
}
