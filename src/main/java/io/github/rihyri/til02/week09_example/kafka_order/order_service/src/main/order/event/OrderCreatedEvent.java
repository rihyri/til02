package io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.event;

/*
 *  [보내는 메시지] 주문이 생성됐다는 이벤트 → order-created 토픽
 *
 *  이벤트 = 서비스끼리 주고받는 "편지 내용"
 *  결제 서비스는 주문 서비스의 DB를 볼 수 없으므로,
 *  결제에 필요한 정보(주문 ID, 금액)를 편지에 담아 보낸다.
 *
 *  결제 서비스에도 똑같은 모양의 클래스가 따로 존재한다.
 *  (서비스끼리 코드를 공유하지 않고, JSON 모양만 맞추는 것이 MSA 방식)
 */
public record OrderCreatedEvent (
        Long orderId,
        String productName,
        long totalAmount
) {
}
