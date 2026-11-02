package io.github.rihyri.til02.week07.project.example.domain.product.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class SyncResult {

    // 신규 등로된 외부 상품 수
    int created;

    // 기존 상품 중 업데이트 수
    int updated;

    // 전체 거리 건수
    public int getTotal() {
        return created + updated;
    }
}
