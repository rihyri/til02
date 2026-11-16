package io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
