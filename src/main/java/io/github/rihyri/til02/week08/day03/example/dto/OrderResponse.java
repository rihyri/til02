package io.github.rihyri.til02.week08.day03.example.dto;

import io.github.rihyri.til02.week08.day03.example.entity.Order;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
public class OrderResponse {

    private final boolean result;
    private final Long orderId;
    private final BigDecimal totalPrice;
    private final String status;

    public OrderResponse (Order order) {
        this.result = true;
        this.orderId = order.getId();
        this.totalPrice = order.getTotalPrice();
        this.status = order.getStatus().name();
    }
}
