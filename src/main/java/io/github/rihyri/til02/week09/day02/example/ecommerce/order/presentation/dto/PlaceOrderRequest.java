package io.github.rihyri.til02.week09.day02.example.ecommerce.order.presentation.dto;

import io.github.rihyri.til02.week09.day02.example.ecommerce.order.application.command.PlaceOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record PlaceOrderRequest(
        @NotNull Long userId,
        @NotEmpty @Valid List<Line> lines,
        @NotNull @Valid Address shippingAddress
) {
    public record Line(@NotNull Long productId, @Positive int quantity) {
    }

    public record Address(@NotBlank String zipCode, @NotBlank String address1, String address2) {
    }

    public PlaceOrderCommand toCommand() {
        return new PlaceOrderCommand(
                userId,
                lines.stream().map(
                        l -> new PlaceOrderCommand.Line(l.productId(), l.quantity())
                ).toList(),
                shippingAddress().zipCode, shippingAddress().address1, shippingAddress().address2
        );
    }
}
