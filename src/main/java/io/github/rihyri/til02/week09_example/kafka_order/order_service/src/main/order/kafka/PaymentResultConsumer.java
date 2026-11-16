package io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.config.KafkaTopicConfig;
import io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.service.OrderService;
import io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.event.PaymentResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/*
 *  [Consumer] 결제 서비스가 보낸 결과를 "받는" 쪽
 *
 *  @KafkaListener(topics = "...")
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    /*
     *  ⑥-1 결제 성공 메시지 수신 → 주문 PAID
     */
    @KafkaListener(topics = KafkaTopicConfig.PAYMENT_COMPLETED, groupId = "order-service")
    public void onPaymentCompleted(String message) throws Exception {
        log.info("[수신] payment-completed: {}", message);
        PaymentResultEvent event = objectMapper.readValue(message, PaymentResultEvent.class);
        orderService.completeOrder(event.orderId());
    }

    /*
     *  ⑥-2 결제 실패 메시지 수신 → 주문 CANCELLED (보상 트랜잭션)
     */
    @KafkaListener(topics = KafkaTopicConfig.PAYMENT_FAILED, groupId = "order-service")
    public void onPaymentFailed(String message) throws Exception {
        log.info("[수신] payment-failed: {}", message);
        PaymentResultEvent event = objectMapper.readValue(message, PaymentResultEvent.class);
        orderService.cancelOrder(event.orderId(), event.reason());
    }
}
