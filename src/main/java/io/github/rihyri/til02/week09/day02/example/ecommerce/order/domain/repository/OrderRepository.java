package io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.repository;

import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model.Order;

import java.util.Optional;

/*
 *  리포지토리 "인터페이스"는 도메인 계층에 둔다. (의존성 역전, DIP)
 *  - 애그리거트 루트(Order) 단위로만 존재한다. OrderLineRepository는 만들지 않는다.
 *  - JPA, MyBatis 등 구현 기술은 infrastructure 계층이 책임진다.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);
}
