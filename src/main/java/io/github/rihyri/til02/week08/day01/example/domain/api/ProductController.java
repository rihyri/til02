package io.github.rihyri.til02.week08.day01.example.domain.api;

import io.github.rihyri.til02.week08.day01.example.domain.dto.ProductResponse;
import io.github.rihyri.til02.week08.day01.example.domain.service.ProductService;
import io.github.rihyri.til02.week08.day01.example.global.annotation.Loggable;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/day08/products")
public class ProductController {

    private final ProductService productService;

    // @Loggable을 붙여 어노테이션 기반 AOP를 확인한다.
    @Loggable
    @GetMapping
    public List<ProductResponse> findAll() {

        return productService.findAll();
    }

    // 어노테이션 없이 실행하여 로그 차이를 비교한다.
    @GetMapping("/{productId}")
    public ProductResponse findById(@PathVariable Long productId) {

        return productService.findById(productId);
    }
}
