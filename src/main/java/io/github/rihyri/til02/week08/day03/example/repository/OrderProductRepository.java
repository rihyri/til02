package io.github.rihyri.til02.week08.day03.example.repository;

import io.github.rihyri.til02.week08.day03.example.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
}
