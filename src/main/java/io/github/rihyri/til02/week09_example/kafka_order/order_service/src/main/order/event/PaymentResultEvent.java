package io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.event;

/*
 *  [받는 메시지] 결제 결과 이벤트 ← payment-completed / payment-failed 토픽
 *
 *  성공이든 실패든 모양은 같고, 어느 토픽으로 왔는지로 성공/실패를 구분한다.
 */
public record PaymentResultEvent (
        Long orderId,
        Long paymentId,     // 실패한 경우에도 결제 시도 기록 ID가 들어 있음
        String reason       // 실패 사유 (성공이면 null)
) {
}