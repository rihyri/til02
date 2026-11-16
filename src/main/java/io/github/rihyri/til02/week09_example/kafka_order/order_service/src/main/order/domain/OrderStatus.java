package io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.domain;

/*
 *  주문 상태
 *
 *  PENDING ──(결제 성공)──▶ PAID
 *      │
 *      └────(결제 실패)──▶ CANCELLED   ← 보상 트랜잭션
 */
public enum OrderStatus {
    PENDING,    // 주문은 저장됐고, 결제 결과를 기다리는 중
    PAID,       // 결제 완료
    CANCELLED   // 결제 실패로 주문 취소
}
