package io.github.rihyri.til02.week09.day02.example.ecommerce.order.presentation.dto;

import jakarta.validation.constraints.Min;

public record ChangeQuantityRequest(@Min(1) int quantity) {
}
