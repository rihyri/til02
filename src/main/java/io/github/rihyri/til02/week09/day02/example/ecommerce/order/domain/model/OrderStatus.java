package io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model;

public enum OrderStatus {

    PENDING,    // 주문 접수 (결제/재고 확인 대기)
    COMPLETED,  // 주문 확정
    CANCELED;    // 주문 취소

    public boolean isModifiable() {
        return this == PENDING;
    }
}
