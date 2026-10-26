package io.github.rihyri.til02.week06.project.coupon.entity;

public enum CouponUserStatus {

    CREATED, // 코드만 발행된 상태
    ISSUED,  // 사용자에게 등록된 상태
    USED     // 사용 완료된 상태
}