package io.github.rihyri.til02.week07.project.example.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class ProductPageResponse {

    private final List<ProductSearchResponse> products;     // 상품 목록
    private final long total;                               // 전체 개수

    private final int page;                                 // 현재 페이지 (1부터)

    @JsonProperty("page_size")
    private final int pageSize;                             // 페이지 크기

    public ProductPageResponse(Page<ProductSearchResponse> result) {
        this.products = result.getContent();
        this.total = result.getTotalElements();
        this.page = result.getNumber() + 1;     // Spring Page는 0부터 시작이라 +1
        this.pageSize = result.getSize();
    }
}
