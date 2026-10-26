package io.github.rihyri.til02.week06.project.coupon.dto.request;

import io.github.rihyri.til02.week06.project.coupon.entity.Coupon;
import io.github.rihyri.til02.week06.project.coupon.entity.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CouponCreateRequest {

    @NotBlank(message = "쿠폰 이름은 필수입니다.")
    @Size(max = 100, message = "쿠폰 이름은 100자 이하여야 합니다.")
    private String coupon_name;

    @NotNull(message = "할인 유형은 필수입니다.")
    private DiscountType discount_type;

    @NotNull(message = "할인 값은 필수입니다.")
    @Positive(message = "할인 값은 0보다 커야 합니다.")
    private Integer discount_value;

    @NotNull(message = "최소 주문 금액은 필수입니다.")
    @PositiveOrZero(message = "최소 주문 금액은 0 이상이어야 합니다.")
    private Integer min_order_amount;

    @Positive(message = "최대 할인 금액은 0보다 커야 합니다.")
    private Integer max_discount_amount;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDateTime start_date;

    @NotNull(message = "종료일은 필수입니다.")
    @Future(message = "종료일은 현재 이후여야 합니다.")
    private LocalDateTime end_date;

    @Positive(message = "사용 한도는 0보다 커야 합니다.")
    private Integer usage_limit;

    /*
     *  시작일과 종료일을 비교하는 사용자 정의 검증
     *  null 여부는 별도의 @NotNull이 검사하도록 밭긴다.
     */
    @AssertTrue(message = "종료일은 시작일 이후여야 합니다.")
    public boolean isValidPeriod() {

        if (start_date == null || end_date == null) {
            return true;
        }

        return end_date.isAfter(start_date);
    }

    // 정률 할인일 때만 1~100% 범위 검증을 수행한다.
    @AssertTrue(message = "정률 할인율은 1~100% 사이어야 합니다.")
    public boolean isValidPercentage() {

        if (discount_type != DiscountType.PERCENTAGE || discount_value == null) {
            return true;
        }

        return discount_value >= 1 && discount_value <= 100;
    }

    // 정액 할인이면서 최소 주문 금액이 0보다 큰 경우에 검증한다.
    @AssertTrue(message = "정액 할인 금액은 최소 주문 금액을 넘을 수 없습니다.")
    public boolean isValidFixedAmount() {

        if (discount_type != DiscountType.FIXED || discount_value == null || min_order_amount == null || min_order_amount == 0) {
            return true;
        }

        return discount_value <= min_order_amount;
    }

    // 요청 DTO를 쿠폰 엔티티로 변환한다.
    public Coupon toEntity() {

        return Coupon.builder()
                .couponName(coupon_name)
                .discountType(discount_type)
                .discountValue(discount_value)
                .minOrderAmount(min_order_amount)

                // 정률 할인에만 최대 할인 금액 정책을 적용한다.
                .maxDiscountAmount(
                        discount_type == DiscountType.PERCENTAGE ? max_discount_amount : 0
                )
                .startDate(start_date)
                .endDate(end_date)
                .usageLimit(usage_limit)
                .build();
    }
}
