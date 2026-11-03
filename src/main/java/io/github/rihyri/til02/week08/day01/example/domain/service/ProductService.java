package io.github.rihyri.til02.week08.day01.example.domain.service;

import io.github.rihyri.til02.week08.day01.example.domain.dto.ProductResponse;
import io.github.rihyri.til02.week08.day01.example.global.exception.ProductNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductService {

    // DB대신 실습용 데이터를 사용한다.
    private final List<ProductResponse> products = List.of(
            new ProductResponse(1L, "노트북", 1200000),
            new ProductResponse(2L, "키보드", 85000),
            new ProductResponse(3L, "마우스", 45000)
    );

    // 전체 상품 목록을 조회한다.
    public List<ProductResponse> findAll() {
        return products;
    }

    // 상품 ID를 기준으로 특정 상품을 조회한다.
    public ProductResponse findById(Long productId) {
        return products.stream()
                .filter(product -> product.getId().equals(productId))
                .findFirst()
                .orElseThrow(() ->
                        new ProductNotFoundException(productId));
    }
}
