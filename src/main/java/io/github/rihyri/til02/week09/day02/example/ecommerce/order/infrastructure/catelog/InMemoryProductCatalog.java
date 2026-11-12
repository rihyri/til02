package io.github.rihyri.til02.week09.day02.example.ecommerce.order.infrastructure.catelog;

import io.github.rihyri.til02.week09.day02.example.ecommerce.order.application.port.ProductCatalog;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/*
 *  ProductCatalog 포트의 임시 구현체
 *  실습을 단순하게 하기 위해 메모리에 상품을 두었고, eureka 예제에서는 Eureka로 product-api를 찾아 호출하는 어댑터로 교체한다.
 *  어떤 구현으로 바꿔도 domain / application 코드는 수정되지 않는다.
 */
@Component
public class InMemoryProductCatalog implements ProductCatalog {

    private static final Map<Long, ProductInfo> PRODUCTS = Map.of(
            1L, new ProductInfo(1L, "커피 쿠폰", BigDecimal.valueOf(5_000)),
            2L, new ProductInfo(2L, "텀블러", BigDecimal.valueOf(25_000)),
            3L, new ProductInfo(3L, "원두 1kg", BigDecimal.valueOf(18_000))
    );

    @Override
    public Optional<ProductInfo> findById(Long productId) {
        return Optional.ofNullable(PRODUCTS.get(productId));
    }
}
