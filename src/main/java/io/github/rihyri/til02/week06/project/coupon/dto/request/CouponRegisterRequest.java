package io.github.rihyri.til02.week06.project.coupon.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CouponRegisterRequest {

    @NotBlank(message = "쿠폰 코드는 필수입니다.")
    private String couponCode;
}