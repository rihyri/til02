package io.github.rihyri.til02.week09.day02.example.ecommerce.order.infrastructure.persistence;

import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model.Order;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 도메인의 OrderRepository 포트를 Spring Data JPA로 구현한 어댑터
@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    @Override
    public Order save(Order order) {
        return jpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpaRepository.findById(id);
    }
}
