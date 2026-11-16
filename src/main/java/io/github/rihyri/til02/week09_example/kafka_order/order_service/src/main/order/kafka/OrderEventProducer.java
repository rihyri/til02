package io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.config.KafkaTopicConfig;
import io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/*
 *  [Producer] Kafka에 메시지를 "보내는" 쪽
 *
 *  KafkaTemplate = Kafka로 메시지를 보내는 도구 (spring-kafka가 자동으로 만들어 줌)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;  // 키 타입, 값 타임
    private final ObjectMapper objectMapper;                    // 객체 ↔ JSON 변환기

    // ③ order-created 토픽으로 "주문 생성됨" 이벤트 발행
    public void sendOrderCreated(OrderCreatedEvent event) {
        String json = toJson(event);

        //  send(토픽, 키, 값)
        //  - 키(orderId): 같은 키는 항상 같은 파티션으로 가서 순서가 보장된다.
        //  - 값(json): 실제 메시지 내용
        kafkaTemplate.send(KafkaTopicConfig.ORDER_CREATED, String.valueOf(event.orderId()), json)
                // send는 비동기라 바로 리턴된다. 결과는 나중에 아래 콜백으로 알 수 있다.
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[발행 실패] orderId = {}", event.orderId(), ex);
                    } else {
                        log.info("[발행 성공] topic = {}, partition = {}, offset = {}, 내용 = {}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),     // 몇 번 칸(파티션)에
                                result.getRecordMetadata().offset(),        // 몇 번째로 저장됐는지
                                json);
                    }
                });
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 변환 실패", e);
        }
    }
}
