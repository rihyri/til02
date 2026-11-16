package io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 *  주문 서비스 (포트 8081)
 *
 *  하는 일
 *  1) 주문 요청을 받아 DB에 PENDING(결제 대기)으로 저장
 *  2) "주문 생성됨" 이벤트를 Kafka에 발행 → 결제 서비스가 받아감
 *  3) 결제 결과 이벤트를 Kafka에서 받아 주문 상태 변경
 *      - 결제 성공 → PAID
 *      - 결제 실패 → CANCELLED (Saga의 보상 트랜잭션)
 */
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
