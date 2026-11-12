package io.github.rihyri.til02.week09.day02.example.ecommerce.order.infrastructure.persistence;

import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderJpaRepository extends JpaRepository<Order, Long> {
}
