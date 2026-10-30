package io.github.rihyri.til02.week07.day04.example.domain.product.controller;

import io.github.rihyri.til02.week07.day04.example.global.external.product.service.ProductExternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/external/products")
public class ProductExternalController {

    private final ProductExternalService productExternalService;

    // 외부 API에서 상품 한 페이지를 조회하여 저장
    @PostMapping("/save")
    public ResponseEntity<String> save() {

        productExternalService.save();
        return ResponseEntity.ok("상품 한 페이지 저장 완료");
    }

    // 외부 API의 모든 상품을 조회하여 저장
    @PostMapping("/save-all")
    public ResponseEntity<String> saveAll() {

        productExternalService.saveAllExternalProducts();
        return ResponseEntity.ok("전체 상품 저장 완료");
    }
}
