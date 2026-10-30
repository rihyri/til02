package io.github.rihyri.til02.week07.day04.example.global.external.product.service;

import io.github.rihyri.til02.global.exception.DomainException;
import io.github.rihyri.til02.global.exception.DomainExceptionCode;
import io.github.rihyri.til02.week07.day04.example.domain.category.entity.Category;
import io.github.rihyri.til02.week07.day04.example.domain.category.repository.CategoryRepository;
import io.github.rihyri.til02.week07.day04.example.domain.product.entity.Product;
import io.github.rihyri.til02.week07.day04.example.domain.product.repository.ProductRepository;
import io.github.rihyri.til02.week07.day04.example.global.external.product.client.ExternalShopClient;
import io.github.rihyri.til02.week07.day04.example.global.external.product.dto.ExternalProductResponse;
import io.github.rihyri.til02.week07.day04.example.global.external.product.dto.ExternalProductResponse.ExternalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductExternalService {

    private final ExternalShopClient externalShopClient;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // ==================================================
    // 1. 외부 상품 한 페이지 조회 후 저장
    // ==================================================
    @Transactional
    @Retryable(
            value = DomainException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void save() {

        try {

            // 외부 API에서 상품 10개 조회
            ExternalProductResponse response
                    = externalShopClient.getProducts(1, 10);

            log.info(response.toString());

            // 응답 데이터에서 실제 상품 목록 추출
            List<ExternalResponse> contents =
                    response.getMessage().getContents();

            // 상품 목록이 비어 있으면 예외 발생
            if (contents.isEmpty()) {
                throw new DomainException(
                        DomainExceptionCode.NOT_FOUND_PRODUCT
                );
            }

            // 외부 상품에 지정할 카테고리 조회
            Category category = categoryRepository.findById(1L)
                    .orElseThrow(() -> new DomainException(
                            DomainExceptionCode.NOT_FOUND_PRODUCT
                    ));

            List<Product> products = new ArrayList<>();

            // 외부 상품 DTO를 내부 Product 엔티티로 변환
            for (ExternalResponse externalProduct : contents) {

                products.add(
                        Product.builder()
                                .name(externalProduct.getName())
                                .description(externalProduct.getDescription())
                                .stock(externalProduct.getStock())
                                .price(externalProduct.getPrice())
                                .category(category)
                                .build()
                );
            }

            // 변환할 상품들을 MySQL에 저장
            productRepository.saveAll(products);

        } catch (Exception error) {

            // 발생한 예외를 DomainException으로 변환
            // Retry 대상 예외이므로 재시도 가능
            throw new DomainException(
                    DomainExceptionCode.NOT_FOUND_PRODUCT
            );
        }
    }


    // ==================================================
    // 2. 페이징을 이용해 외부 상품 전체 저장
    // ==================================================
    @Transactional
    @Retryable(
            value = DomainException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void saveAllExternalProducts() {

        log.info("=== 전체 상품 저장 시작 ===");

        // 첫 번째 페이지부터 조회
        int page = 0;

        // 한 번에 조회할 상품 개수
        int pageSize = 10;

        // 마지막 페이지 확인 변수
        boolean lastPage = false;

        // 마지막 페이지를 조회할 때까지 반복
        while (!lastPage) {

            // 외부 API에서 현재 상품페이지 상품 조회
            ExternalProductResponse responses =
                    externalShopClient.getProducts(page, pageSize);

            log.info(
                    "Response for page {}: {}",
                    page,
                    responses
            );

            // 응답 자체가 없거나 message가 없으면 예외 발생
            if (responses == null || responses.getMessage() == null) {

                throw new DomainException(
                        DomainExceptionCode.NOT_FOUND_PRODUCT
                );
            }

            // 현재 페이지에 포함된 상품 목록
            List<ExternalResponse> contents = responses.getMessage().getContents();;

            // 더 이상 상품이 없으면 반복 종료
            if (contents == null || contents.isEmpty()) {
                break;
            }

            // 실습에서는 임시로 카테고리 ID 1을 사용
            Category category = categoryRepository.findById(1L)
                    .orElseThrow(() -> new DomainException(
                            DomainExceptionCode.NOT_FOUND_PRODUCT
                    ));

            List<Product> products = new ArrayList<>();

            // 외부 DTO를 내부 Products 엔티티로 변환
            for (ExternalResponse externalProduct : contents) {

                Product product = Product.builder()
                        .name(externalProduct.getName())
                        .description(externalProduct.getDescription())
                        .stock(externalProduct.getStock())
                        .price(externalProduct.getPrice())
                        .category(category)
                        .build();

                products.add(product);
            }

            // 현재 페이지 상품들을 MySQL에 저장
            productRepository.saveAll(products);

            // 응답에 포함된 페이징 정보 확인
            ExternalProductResponse.ExternalPageable pageable =
                    responses.getMessage().getPageable();

            if (pageable != null) {

                // 서버가 알려주는 마지막 페이지 여부
                lastPage = pageable.isLast();
            } else {

                // 페이징 정보가 없을 경우 개수로 판단
                lastPage = contents.size() < pageSize;
            }

            // 다음 페이지 조회
            page++;
        }
    }
}
