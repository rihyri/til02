package io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 같은 주문에 대한 결제가 이미 있는지 확인 (중복 메시지 대비)
    boolean existsByOrderId(Long orderId);
}
