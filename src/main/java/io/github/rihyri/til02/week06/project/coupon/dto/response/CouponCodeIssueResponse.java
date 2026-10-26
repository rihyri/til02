package io.github.rihyri.til02.week06.project.coupon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
public class CouponCodeIssueResponse {

    private final Long couponId;
    private final int issuedCount;
    private final List<String> codes;
}
