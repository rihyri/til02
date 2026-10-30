package io.github.rihyri.til02.week07.day04.example.global.external.product.client;

import io.github.rihyri.til02.week07.day04.example.global.config.OpenFeignConfig;
import io.github.rihyri.til02.week07.day04.example.global.external.product.dto.ExternalProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name="external-products",
        url = "${external.external-shop.url}",
        configuration = OpenFeignConfig.class
)
public interface ExternalShopClient {

    // 외부 서버의 상품 목록을 페이지 단위로 조회
    @GetMapping("/products")
    ExternalProductResponse getProducts (
      // 조회할 페이지 번호
      @RequestParam("page") Integer page,
      // 페이지당 상품 개수
      @RequestParam("size") Integer size
    );
}
