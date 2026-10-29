package io.github.rihyri.til02.week07.day03.example.controller;

import io.github.rihyri.til02.week07.day03.example.dto.OrderRequest;
import io.github.rihyri.til02.week07.day03.example.service.OrderInfoService;
import io.github.rihyri.til02.week07.day03.example.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/week7/demo")
@RequiredArgsConstructor
public class OrderDemoController {

    private final OrderService orderService;
    private final OrderInfoService orderInfoService;

    // REQUIRED 실습
    @PostMapping("/required")
    public ResponseEntity<String> required(@RequestBody OrderRequest request) {

        orderService.required(request);

        return ResponseEntity.ok("완료");
    }

    // REQUIRES_NEW 실습
    @PostMapping("/requires-new")
    public ResponseEntity<String> requiresNew(@RequestBody OrderRequest request) {

        orderService.requiresNew(request);

        return ResponseEntity.ok("완료");
    }

    // NESTED 실습
    @PostMapping("/nested")
    public ResponseEntity<String> nested(@RequestBody OrderRequest request) {

        orderService.nested(request);

        return ResponseEntity.ok("부모 작업만 커밋");
    }

    // SUPPORTS: 부모 트랜잭션 없이 조회
    @GetMapping("/supports/outside/{userId}")
    public String outside(@PathVariable long userId) {

        return orderInfoService.getUser(userId);
    }

    // SUPPORTS: 부모 트랜잭션 안에서 조회
    @GetMapping("/supports/inside/{userId}")
    public String async(@RequestBody OrderRequest request) {

        return "요청 큐 번호: " + orderService.orderRequestAsync(request);
    }
}
