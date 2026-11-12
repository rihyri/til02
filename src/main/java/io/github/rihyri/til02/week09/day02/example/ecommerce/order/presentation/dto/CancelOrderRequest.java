package io.github.rihyri.til02.week09.day02.example.ecommerce.order.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest (@NotBlank String reason) {
}
