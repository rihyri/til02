package io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.config;

import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/*
 *  토픽(Topic) 생성 설정
 *
 *  토픽 = 메시지를 종류별로 나눠 담는 "우편함".
 *  서비스끼리는 서로의 주소를 모르고, "토픽 이름"만 약속해서 통신한다.
 *  → 이 프로젝트에서 서비스를 이어주는 연결고리는 아래 3개의 토픽 이름이다.
 *
 *  NewTopic을 Bean으로 등록하면 서비스가 켜질 때 토픽이 없으면 자동으로 만들어준다.
 */
@Configuration
public class KafkaTopicConfig {

    // 토픽 이름은 오타가 나면 통신이 안 되므로 상수로 모아 둔다.
    public static final String ORDER_CREATED = "order-created";         // 주문 → 결제
    public static final String PAYMENT_COMPLETED = "payment-completed"; // 결제 → 주문
    public static final String PAYMENT_FAILED = "payment-failed";       // 결제 → 주문

    @Bean
    public NewTopic orderCreatedTopic () {
        // partitions(3): 토픽을 3칸으로 나눠 병렬 처리 가능
        // replicas(1): 브로커가 1대라 복제본도 1개
        return TopicBuilder.name(ORDER_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(PAYMENT_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(PAYMENT_FAILED).partitions(3).replicas(1).build();
    }
}
