package io.github.rihyri.til02.week08.day03.example.controller;

import io.github.rihyri.til02.week08.day03.example.dto.OrderCreateRequest;
import io.github.rihyri.til02.week08.day03.example.dto.OrderResponse;
import io.github.rihyri.til02.week08.day03.example.entity.Order;
import io.github.rihyri.til02.week08.day03.example.entity.User;
import io.github.rihyri.til02.week08.day03.example.repository.UserRepository;
import io.github.rihyri.til02.week08.day03.example.service.OrderProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/day08/orders")
public class OrderController {

    private final OrderProcessService orderProcessService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderCreateRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다.")
                );

        Order order = orderProcessService.process(
                user,
                request.getProduct()
        );

        return ResponseEntity.ok(new OrderResponse(order));
    }
}
