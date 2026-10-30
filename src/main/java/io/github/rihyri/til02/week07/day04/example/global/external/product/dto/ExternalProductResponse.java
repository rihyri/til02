package io.github.rihyri.til02.week07.day04.example.global.external.product.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExternalProductResponse {

    // 외부 API 요청 성공 여부
    Boolean result;

    // 요청 실패 시 반환되는 오류
    ExternalError error;

    // 상품 목록과 페이징 정보
    ExternalPage message;

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ExternalError {

        String errorCode;
        String errorMessage;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ExternalPage {

        // 현재 페이지의 상품 목록
        List<ExternalResponse> contents;

        // 페이지 번호 및 마지막 페이지 여부
        ExternalPageable pageable;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ExternalPageable {

        Long offset;
        Long pageNumber;
        Long pageSize;
        Long pageElements;
        Long totalPages;
        Long totalElements;

        // 첫 번째 페이지 여부
        boolean first;

        // 마지막 페이지 여부
        boolean last;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ExternalResponse {

        Long id;
        String name;
        String description;
        BigDecimal price;
        Integer stock;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;
    }
}
