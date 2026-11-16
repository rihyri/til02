package io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 *  결제 서비스 (포트 8082)
 *
 *  하는 일
 *  1) order-created 토픽에서 "주문 생성됨" 이벤트를 받음
 *  2) 결제 처리 (예제 규칙: 금액이 100,000원을 넘으면 잔액 부족으로 실패)
 *  3) 결과를 payment-completed 또는 payment-failed 토픽에 발행 → 주문 서비스가 받아감
 */
@SpringBootApplication
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
