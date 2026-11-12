package io.github.rihyri.til02.week09.day02.example.ecommerce.order.application.port;

import java.math.BigDecimal;
import java.util.Optional;

/*
 *  외부 컨텍스트(Product)에 대한 "포트"
 *  주문 컨텍스트는 상품 서비스의 내부 모델을 모르고, 주문에 필요한 정보(이름, 가격)만 이 인터페이스로 받는다.
 *  실제 구현(REST 호출, Eureka, 캐시 등)은 infrastructure의 어댑터가 담당한다. → Anti-Corruption Layer
 */
public interface ProductCatalog {

    Optional<ProductInfo> findById(Long productId);

    record ProductInfo(Long productId, String name, BigDecimal price) {
    }
}
