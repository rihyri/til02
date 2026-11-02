package io.github.rihyri.til02.week07.project.example.domain.product.service;

import io.github.rihyri.til02.global.exception.DomainException;
import io.github.rihyri.til02.global.exception.DomainExceptionCode;
import io.github.rihyri.til02.week07.project.example.domain.category.dto.CategoryRepository;
import io.github.rihyri.til02.week07.project.example.domain.category.entity.Category;
import io.github.rihyri.til02.week07.project.example.domain.product.entity.Product;
import io.github.rihyri.til02.week07.project.example.domain.product.entity.SyncResult;
import io.github.rihyri.til02.week07.project.example.domain.product.repository.ProductRepository;
import io.github.rihyri.til02.week07.project.example.global.external.dto.ExternalProductResponse.ExternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductUpsertService {

    private static final String EXTERNAL_CATEGORY = "외부상품";
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public SyncResult upsert(List<ExternalResponse> externals) {

        Category category = categoryRepository.findByName(EXTERNAL_CATEGORY)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.NOT_FOUND_CATEGORY));

        List<String> ids = externals.stream()
                .map(
                        e -> String.valueOf(e.getId())
                ).toList();

        Map<String, Product> existing = productRepository.findAllByExternalProductIdIn(ids).stream()
                .collect(Collectors.toMap(Product::getExternalProductId, Function.identity()));

        List<Product> toInsert = new ArrayList<>();
        int updated = 0;

        for (ExternalResponse e : externals) {
            String extId = String.valueOf(e.getId());
            Product found = existing.get(extId);

            if (found == null) {
                toInsert.add(Product.fromExternal(extId, category, e.getName(), e.getDescription(), e.getPrice(), e.getStock()));
            } else {
                found.syncFromExternal(e.getName(), e.getDescription(), e.getPrice(), e.getStock());
                updated++;
            }
        }

        productRepository.saveAll(toInsert);
        return new SyncResult(toInsert.size(), updated);
    }
}
