package io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 *  결제 엔티티
 *
 *  orderId는 주문 서비스의 주문 ID를 "값으로만" 들고 있다.
 *  DB가 분리되어 있어서 외래키(FK)나 JPA 연관관계(@ManyToOne)로 연결할 수 없다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)      // 주문 하나 당 결제는 하나 (중복 결제 방지)
    private Long orderId;

    private long amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String failReason;

    private LocalDateTime createdAt;

    public static Payment completed(Long orderId, long amount) {
        return of(orderId, amount, PaymentStatus.COMPLETED, null);
    }

    public static Payment failed(Long orderId, long amount, String reason) {
        return of(orderId, amount, PaymentStatus.FAILED, reason);
    }

    private static Payment of(Long orderId, long amount, PaymentStatus status, String reason) {
        Payment p = new Payment();
        p.orderId = orderId;
        p.amount = amount;
        p.status = status;
        p.failReason = reason;
        p.createdAt = LocalDateTime.now();
        return p;
    }
}
