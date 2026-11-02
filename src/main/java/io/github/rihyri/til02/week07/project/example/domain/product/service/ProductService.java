package io.github.rihyri.til02.week07.project.example.domain.product.service;

import io.github.rihyri.til02.week07.project.example.domain.product.dto.ProductSearchResponse;
import io.github.rihyri.til02.week07.project.example.domain.product.repository.ProductQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductQueryRepository productQueryRepository;

    @Transactional(readOnly = true)
    public Page<ProductSearchResponse> search(String category, Boolean isOrderable, Pageable pageable) {
        return productQueryRepository.search(category, isOrderable, pageable);
    }
}
