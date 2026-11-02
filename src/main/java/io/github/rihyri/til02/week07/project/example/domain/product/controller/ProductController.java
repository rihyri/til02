package io.github.rihyri.til02.week07.project.example.domain.product.controller;

import io.github.rihyri.til02.week07.project.example.domain.product.dto.ProductPageResponse;
import io.github.rihyri.til02.week07.project.example.domain.product.dto.ProductSearchResponse;
import io.github.rihyri.til02.week07.project.example.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 예: GET   /products?category=외부상품&is_orderable=true&page=1&page_size=20
    @GetMapping("/products")
    public ResponseEntity<ProductPageResponse> search(
            @RequestParam(required = false) String category,
            @RequestParam(name = "is_orderable", required = false) Boolean isOrderable,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize
    ) {

        // 사용자는 1페이지부터 요청하지만 Spring은 0부터 세기 때문에 -1
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize);

        Page<ProductSearchResponse> result = productService.search(category, isOrderable, pageable);

        return ResponseEntity.ok(new ProductPageResponse(result));
    }
}
