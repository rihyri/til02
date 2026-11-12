package io.github.rihyri.til02.week09.day02.example.ecommerce.order.application;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long orderId) {
        super("주문을 찾을 수 없습니다. orderId = " + orderId);
    }
}
