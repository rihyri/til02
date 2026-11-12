package io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 도메인 이벤트는 "이미 일어난 사실"이므로 과거형으로 이름 짓고, 불변 객체로 만든다.
public record OrderPlacedEvent (Long orderId, Long userId, BigDecimal totalAmount, LocalDateTime occurredAt) {
}
