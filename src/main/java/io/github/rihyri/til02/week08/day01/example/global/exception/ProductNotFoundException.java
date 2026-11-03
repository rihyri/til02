package io.github.rihyri.til02.week08.day01.example.global.exception;

import lombok.Getter;

@Getter
public class ProductNotFoundException extends RuntimeException {

    private final String code;

    public ProductNotFoundException(Long productId) {
        super("상품을 찾을 수 없습니다. ID: " + productId);
        this.code = "PRODUCT_NOT_FOUND";
    }
}
