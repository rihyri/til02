package io.github.rihyri.til02.week06.project.product.controller;

import io.github.rihyri.til02.global.response.ApiResponse;
import io.github.rihyri.til02.week06.project.product.dto.ProductMaxDiscountResponse;
import io.github.rihyri.til02.week06.project.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/project/products")
public class ProductController {

    private final ProductService productService;

    /*
    @GetMapping("/{productId}/max-discount")
    public ApiResponse<ProductMaxDiscountResponse> getMaxDiscount(@PathVariable Long productId, Authentication authentication) {

        // 현재 로그인한 사용자의 쿠폰을 기준으로 할인 금액을 계산한다.
        User user = authUserUtil.getAuthUser(authentication);

        return ApiResponse.ok(
            productService.calculateBestDiscountPrice(productId, user);
        );
    }
    */
}
