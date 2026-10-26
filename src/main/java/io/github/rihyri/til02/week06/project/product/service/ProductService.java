package io.github.rihyri.til02.week06.project.product.service;

import io.github.rihyri.til02.global.exception.DomainException;
import io.github.rihyri.til02.global.exception.DomainExceptionCode;
import io.github.rihyri.til02.week06.project.coupon.repository.CouponUserRepository;
import io.github.rihyri.til02.week06.project.product.dto.ProductMaxDiscountResponse;
import io.github.rihyri.til02.week06.project.product.entity.Product;
import io.github.rihyri.til02.week06.project.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CouponUserRepository couponUserRepository;

    /*
     *  사용자가 보유한 쿠폰 중 상품 가격을 가장 많이 할인해 주는
     *  쿠폰을 찾아 최종 가격을 계산한다.
     *
     *  쿠폰을 실제 사용하는 작업이 아니라 조회 및 계산만 수행한다.
     */

    @Transactional(readOnly = true)
    public ProductMaxDiscountResponse calculateBestDiscountPrice(Long productId)
    {

        // 1. 할인 금액을 계산할 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new DomainException(
                        DomainExceptionCode.NOT_FOUND_PRODUCT
                ));

        BigDecimal price = product.getPrice();

        // 임시 아이디
        Long userId = 1L;

        // 2. 사용자가 보유한 사용 가능한 쿠폰 목록 조회
        return couponUserRepository
                .findUsableCoupons(userId, LocalDateTime.now())
                .stream()

                // 3. 최소 주문 금액 조건을 충족하는 쿠폰만 필터링
                .filter(cu ->
                        cu.getCoupon().meetsMinOrderAmount(price)
                )

                // 4. 실제 할인 금액이 가장 높은 쿠폰을 선택
                .max(Comparator.comparing(
                        cu -> cu.getCoupon().calculateDiscount(price)
                ))

                // 5. 쿠폰이 있으면 할인 금액 및 최종 가격 반환
                .map(best ->
                        ProductMaxDiscountResponse.of(
                                product, best, best.getCoupon().calculateDiscount(price)
                        )
                )

                // 6. 적용 가능한 쿠폰이 없으면 할인 없는 가격 반환
                .orElseGet(() ->
                        ProductMaxDiscountResponse.noCoupon(product)
                );
    }

}
