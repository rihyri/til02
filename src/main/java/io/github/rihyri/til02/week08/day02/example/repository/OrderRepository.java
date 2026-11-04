package io.github.rihyri.til02.week08.day02.example.repository;

import io.github.rihyri.til02.week08.day02.example.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
