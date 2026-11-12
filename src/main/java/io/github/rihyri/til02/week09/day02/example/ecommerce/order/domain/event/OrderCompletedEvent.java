package io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.event;

import java.time.LocalDateTime;

public record OrderCompletedEvent(Long orderId, LocalDateTime occurredAt) {
}
