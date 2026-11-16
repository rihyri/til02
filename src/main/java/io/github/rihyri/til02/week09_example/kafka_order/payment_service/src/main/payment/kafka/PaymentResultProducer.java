package io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.event.PaymentResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/*
 *  [Producer] 결제 결과를 Kafka에 "보내는" 쪽
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultProducer {

    // 토픽 이름: 주문 서비스의 KafkaTopicConfig와 똑같은 이름이어야 한다. (약속)
    private static final String PAYMENT_COMPLETED = "payment-completed";
    private static final String PAYMENT_FAILED = "payment-failed";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // ① 결제 성공 발행
    public void sendCompleted(PaymentResultEvent event) {
        send(PAYMENT_COMPLETED, event);
    }

    // ② 결제 실패 발행 → 주문 서비스가 받으면 보상(주문 취소)을 실행
    public void sendFailed(PaymentResultEvent event) {
        send(PAYMENT_FAILED, event);
    }

    private void send(String topic, PaymentResultEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, String.valueOf(event.orderId()), json);
            log.info("[발행] {}: {}", topic, json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 변환 실패", e);
        }
    }
}
