package io.github.rihyri.til02.week09.day02.example.ecommerce.order.presentation;

import io.github.rihyri.til02.week09.day02.example.ecommerce.order.application.OrderService;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.presentation.dto.CancelOrderRequest;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.presentation.dto.ChangeQuantityRequest;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.presentation.dto.OrderResponse;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.presentation.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/orders")
@RequiredArgsConstructor
@RestController
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return OrderResponse.from(orderService.placeOrder(request.toCommand()));
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        return OrderResponse.from(orderService.getOrder(orderId));
    }

    @PatchMapping("/{orderId}/shipping-address")
    public OrderResponse changeShippingAddress(@PathVariable Long orderId,
                                               @Valid @RequestBody PlaceOrderRequest.Address request) {
        return OrderResponse.from(orderService.changeShippingAddress(
                orderId, request.zipCode(), request.address1(), request.address2()));
    }

    @PatchMapping("/{orderId}/lines/{productId}")
    public OrderResponse changeQuantity(@PathVariable Long orderId, @PathVariable Long productId,
                                        @Valid @RequestBody ChangeQuantityRequest request) {
        return OrderResponse.from(orderService.changeQuantity(orderId, productId, request.quantity()));
    }

    @PostMapping("/{orderId}/complete")
    public OrderResponse completeOrder(@PathVariable Long orderId) {
        return OrderResponse.from(orderService.completeOrder(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long orderId, @Valid @RequestBody CancelOrderRequest request) {
        return OrderResponse.from(orderService.cancelOrder(orderId, request.reason()));
    }
}
