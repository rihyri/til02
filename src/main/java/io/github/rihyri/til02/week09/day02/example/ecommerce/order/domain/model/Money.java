package io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model;

import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/*
 *  Value Object : 식별자가 없고 "값" 자체가 정체성이다.
 *  - 불변(immutable) : 연산 결과는 항상 새 객체로 반환한다.
 *  - 값 동등성 : record가 equals/hashCode를 모든 필드 기준으로 자동 생성한다.
 *  - 자가 검증 : 음수 금액 같은 잘못된 값은 생성 시점에 거부한다.
 */
@Embeddable
public record Money(BigDecimal amount) {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다: " + amount);
        }
    }

    public static Money won(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money times(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
    }

    public boolean isGreaterThanOrEqual (Money other) {
        return amount.compareTo(other.amount) >= 0;
    }
}
