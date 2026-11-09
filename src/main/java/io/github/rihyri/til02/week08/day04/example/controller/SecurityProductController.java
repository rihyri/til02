package io.github.rihyri.til02.week08.day04.example.controller;

import io.github.rihyri.til02.week08.day04.example.dto.SecurityProductResponse;
import io.github.rihyri.til02.week08.day04.example.dto.SecurityUserCreateRequest;
import io.github.rihyri.til02.week08.day04.example.repository.SecurityProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/security")
public class SecurityProductController {

    private final SecurityProductRepository productRepository;

    // 상품명으로 조회
    @GetMapping("/products/search")
    public List<SecurityProductResponse> searchByName(@RequestParam String name) {
        return productRepository.searchByName(name);
    }

    // 허용된 컬럼으로 상품 정렬
    @GetMapping("/products")
    public List<SecurityProductResponse> searchAll(@RequestParam(defaultValue = "name") String sort) {
        return productRepository.searchAll(sort);
    }

    // 입력값 검증을 위한 실습용 API
    public ResponseEntity<Map<String, String>> validateUser(@Valid @RequestBody SecurityUserCreateRequest request) {
        return ResponseEntity.ok(Map.of("message", "입력값 검증에 성공했습니다."));
    }
}
