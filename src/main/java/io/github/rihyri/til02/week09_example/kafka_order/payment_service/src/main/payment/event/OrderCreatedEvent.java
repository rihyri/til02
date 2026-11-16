package io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.event;

/*
 *  [받는 메시지] 주문 생성 이벤트 ← order-created 토픽
 *
 *  주문 서비스에 있는 OrderCreatedEvent와 "필드 이름 타입"이 같아야
 *  JSON → 객체 변환이 된다. (패키지는 달라도 상관없음)
 */
public record OrderCreatedEvent (
        Long orderId,
        String productName,
        long totalAmount
) {
}
