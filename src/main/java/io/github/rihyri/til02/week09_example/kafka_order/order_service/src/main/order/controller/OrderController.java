package io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.controller;

import io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.domain.Order;
import io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/*
 *  주문 API
 *
 *  POST  /orders       주문 생성       ← 흐름의 시작점 ①
 *  GET   /orders/{id}  주문 1건 조회   (상태가 바뀌었는지 확인용)
 *  GET   /orders       전체 주문 조회
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 요청 바디: {"productName":"keyboard", "quantity":1, "price":500000}
    public record CreateOrderRequest(String productName, int quantity, long price) {
    }

    // 응답: 엔티티를 그대로 내보내지 않고 필요한 값만 담은 DTO
    public record OrderResponse(Long id, String productName, int quantity, long totalAmount, String status, String cancelReason) {
        static OrderResponse from (Order o) {
            return new OrderResponse(o.getId(), o.getProductName(), o.getQuantity(), o.getTotalAmount(), o.getStatus().name(), o.getCancelReason());
        }
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return OrderResponse.from(orderService.getOrder(id));
    }

    @GetMapping
    public List<OrderResponse> getAll() {
        return orderService.getAllOrders().stream().map(OrderResponse::from).toList();
    }
}
