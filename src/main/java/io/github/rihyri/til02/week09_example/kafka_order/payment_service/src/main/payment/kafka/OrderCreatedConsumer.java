package io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.event.OrderCreatedEvent;
import io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/*
 *  [Consumer] 주문 서비스가 보낸 "주문 생성됨" 메시지를 받는 쪽
 *
 *  ⭐ 결제 서비스의 모든 일은 이 메서드에서 시작된다. (결제 서비스의 시작됨)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    /*
     *  ④ order-created 토픽 구독
     *
     *  파라미터를 String 대신 ConsumerRecord로 받으면
     *  메시지 내용(value)뿐 아니라, 키, 파티션, 오프셋까지 볼 수 있다.
     */
    @KafkaListener(topics = "order-created", groupId = "payment-service")
    public void onOrderCreated(ConsumerRecord<String, String> record) throws Exception {
        log.info("[수신] order-created: key = {}, partition = {}, offset = {}, 내용 = {}",
                record.key(), record.partition(), record.offset(), record.value());

        // JSON 문자열 → 객체
        OrderCreatedEvent event = objectMapper.readValue(record.value(), OrderCreatedEvent.class);
        paymentService.pay(event);

        // 메서드가 예외 없이 끝나면 Spring이 Kafka에 "여기까지 읽었음(오프셋 커밋)"을 알린다.
        // 그래서 서비스를 재시작해도 이 메시지를 다시 받지 않는다.
    }
}
