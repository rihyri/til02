package io.github.rihyri.til02.week09.day02.example.ecommerce.order.presentation.dto;

import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model.Order;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model.ShippingAddress;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        Long userId,
        String status,
        List<Line> lines,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        ShippingAddress shippingAddress,
        String cancelReason,
        LocalDateTime orderedAt
) {
    public record Line(Long productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal amount) {
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus().name(),
                order.getLines().stream()
                        .map(l -> new Line(l.getProductId(), l.getProductName(), l.getUnitPrice().amount(),
                                l.getQuantity(), l.amount().amount()))
                        .toList(),
                order.getShippingFee().amount(),
                order.getTotalAmount().amount(),
                order.getShippingAddress(),
                order.getCancelReason(),
                order.getOrderedAt());
    }
}
