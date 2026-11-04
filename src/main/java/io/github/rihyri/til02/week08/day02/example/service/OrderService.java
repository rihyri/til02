package io.github.rihyri.til02.week08.day02.example.service;

import io.github.rihyri.til02.week08.day02.example.domain.Order;
import io.github.rihyri.til02.week08.day02.example.domain.OrderStatus;
import io.github.rihyri.til02.week08.day02.example.exception.OrderException;
import io.github.rihyri.til02.week08.day02.example.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    // ========== 리팩토링 전 ==========
    /*
    @Transactional
    public void cancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("존재하지 않는 주문입니다."));

        // 주문 취소 조건 검증
        if (order.getStatus() != OrderStatus.CANCELED) {
            throw new OrderException("취소할 수 없는 주문입니다.");
        }

        // 주문 상태 변경
        order.setStatus(OrderStatus.CANCELED);
    }
     */

    // ========== 리팩토링 후 ==========
    @Transactional
    public void cancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("존재하지 않는 주문입니다."));

        // 취소 가능 여부는 엔티티가 직접 판단한다.
        order.cancelOrder();
    }
}
