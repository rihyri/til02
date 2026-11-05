package io.github.rihyri.til02.week08.day03.example.repository;

import io.github.rihyri.til02.week08.day03.example.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
