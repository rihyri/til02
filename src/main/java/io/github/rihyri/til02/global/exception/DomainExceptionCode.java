package io.github.rihyri.til02.global.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum DomainExceptionCode {

    NOT_FOUND_CATEGORY(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    CATEGORY_HAS_CHILDREN(HttpStatus.BAD_REQUEST, "하위 카테고리가 존재하여 삭제할 수 없습니다."),

    NOT_FOUND_PRODUCT(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    OUT_OF_STOCK_PRODUCT(HttpStatus.BAD_REQUEST, "재고 수량이 없습니다."),

    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "중복된 이메일입니다."),

    // 쿠폰 관련
    NOT_FOUND_COUPON(HttpStatus.NOT_FOUND, "쿠폰 정보를 찾을 수 없습니다."),
    INVALID_COUPON_DATE(HttpStatus.BAD_REQUEST, "종료일시는 시작일시보다 빠를 수 없습니다."),
    INVALID_COUPON_PERIOD(HttpStatus.BAD_REQUEST, "쿠폰을 사용할 수 있는 기간이 아닙니다."),
    EXCEEDED_MAX_PERCENT(HttpStatus.BAD_REQUEST, "정률 할인(PERCENT)인 경우 할인 값은 100%를 초과할 수 없습니다."),
    MISSING_MAX_DISCOUNT_AMOUNT(HttpStatus.BAD_REQUEST, "정률 할인(PERCENT)인 경우 최대 할인 한도(maxDiscountAmount)를 설정해야 합니다."),

    ALREADY_REGISTERED_COUPON(HttpStatus.BAD_REQUEST, "이미 사용한 쿠폰입니다."),
    UNAVAILABLE_COUPON(HttpStatus.BAD_REQUEST, "사용 기간이 지난 쿠폰입니다."),
    EXCEED_COUPON_USAGE_LIMIT(HttpStatus.BAD_REQUEST, "쿠폰 발행 한도를 초과했습니다."),
    NOT_FOUND_COUPON_USER(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    NOT_FOUND_COUPON_CODE(HttpStatus.NOT_FOUND, "우효한 쿠폰 코드가 존재하지 않습니다."),
    INVALID_COUPON_CODE(HttpStatus.UNPROCESSABLE_ENTITY, "해당 쿠폰 코드는 사용할 수 없습니다."),
    ALREADY_USED_COUPON(HttpStatus.UNPROCESSABLE_ENTITY, "이미 사용된 쿠폰 코드 입니다."),
    INACTIVE_COUPON_ID(HttpStatus.UNPROCESSABLE_ENTITY, "해당 쿠폰는 비활성화 되있습니다."),

    ALREADY_MAPPED_COUPON(HttpStatus.CONFLICT, "이미 적용 또는 발급된 쿠폰입니다.");


    final HttpStatus status;
    final String message;
}