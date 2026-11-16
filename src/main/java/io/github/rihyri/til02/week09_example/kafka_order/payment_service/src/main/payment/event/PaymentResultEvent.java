package io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.event;

/*
 *  [보내는 메시지] 결제 결과 이벤트 → payment-completed / payment-failed 토픽
 */
public record PaymentResultEvent (
        Long orderId,
        Long paymentId,
        String reason       // 실패 사유 (성공이면 null)
) {
}