package io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model;

import jakarta.persistence.Embeddable;

// Value Object : 배송지, 주소의 일부만 바꾸는 setter는 없고 바꾸려면 새 주소로 "통째로 교체"한다.
@Embeddable
public record ShippingAddress(String zipCode, String address1, String address2) {

    public ShippingAddress {
        if (zipCode == null || zipCode.isBlank()) {
            throw new IllegalArgumentException("우편번호는 필수입니다.");
        }

        if (address1 == null || address1.isBlank()) {
            throw new IllegalArgumentException("기본 주소는 필수입니다.");
        }
    }
}
