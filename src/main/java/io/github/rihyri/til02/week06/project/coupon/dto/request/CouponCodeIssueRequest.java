package io.github.rihyri.til02.week06.project.coupon.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CouponCodeIssueRequest {

    // 과도한 대량 발행을 방지하기 위해 요청 수량을 제한한다.
    @NotNull(message = "발행 개수는 필수입니다.")
    @Min(value = 1, message = "발행 개수는 1개 이상이어야 합니다.")
    @Max(value = 1000, message = "한 번에 최대 1000개까지 발행할 수 있습니다.")
    private Integer count;
}