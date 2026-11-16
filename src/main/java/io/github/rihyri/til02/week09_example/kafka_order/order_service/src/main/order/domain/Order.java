package io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 *  주문 엔티티
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private int quantity;
    private long price;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String cancelReason;    // 취소 사유
    private LocalDateTime createdAt;

    // 주문 생성: 처음에는 항상 PENDING
    public static Order create(String productName, int quantity, long price) {
        Order order = new Order();
        order.productName = productName;
        order.quantity = quantity;
        order.price = price;
        order.status = OrderStatus.PENDING;
        order.createdAt = LocalDateTime.now();
        return order;
    }

    public long getTotalAmount() {
        return price * quantity;
    }

    // 결제 성공 시 호출
    public void markPaid() {
        this.status = OrderStatus.PAID;
    }

    // 결제 실패 시 호출 (보상 트랜잭션: 이미 저장된 주문을 "되돌림")
    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
    }
}
