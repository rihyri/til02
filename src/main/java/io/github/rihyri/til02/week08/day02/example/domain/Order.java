package io.github.rihyri.til02.week08.day02.example.domain;

import io.github.rihyri.til02.week08.day02.example.exception.OrderException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    // ========== 풍부한 도메인 모델 ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    // 상태 변경 규칙을 엔티티 안에서 관리한다.
    public void cancelOrder() {

        if (this.status != OrderStatus.PENDING) {
            throw new OrderException("취소할 수 없는 주문입니다.");
        }

        this.status = OrderStatus.CANCELED;
    }
}
