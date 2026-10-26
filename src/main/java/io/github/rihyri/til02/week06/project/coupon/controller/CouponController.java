package io.github.rihyri.til02.week06.project.coupon.controller;

import com.oracle.svm.core.annotate.Delete;
import io.github.rihyri.til02.global.response.ApiResponse;
import io.github.rihyri.til02.week06.project.coupon.dto.request.CouponCodeIssueRequest;
import io.github.rihyri.til02.week06.project.coupon.dto.request.CouponCreateRequest;
import io.github.rihyri.til02.week06.project.coupon.dto.request.CouponRegisterRequest;
import io.github.rihyri.til02.week06.project.coupon.dto.response.CouponCodeIssueResponse;
import io.github.rihyri.til02.week06.project.coupon.dto.response.CouponResponse;
import io.github.rihyri.til02.week06.project.coupon.dto.response.CouponUserResponse;
import io.github.rihyri.til02.week06.project.coupon.dto.response.PageResponse;
import io.github.rihyri.til02.week06.project.coupon.service.CouponService;
import io.github.rihyri.til02.week06.project.coupon.service.CouponUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/project/coupons")
public class CouponController {

    private final CouponService couponService;
    private final CouponUserService couponUserService;
    // private final AuthUserUtil authUserUtil;

    // 쿠폰 상세 조회
    @GetMapping("/{couponId}")
    public ResponseEntity<ApiResponse<CouponResponse>> getCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.getCoupon(couponId)));
    }

    // 쿠폰 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CouponResponse>>> getCoupons(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Boolean isActive
    ) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.getCoupons(isActive, page, size)));
    }

    // 쿠폰 생성
    @PostMapping
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @Valid @RequestBody CouponCreateRequest request
    ) {
        CouponResponse response = couponService.createCoupon(request);

        // 생성된 리소스의 URI를 Location 헤더에 포함한다.
        return ResponseEntity
                .created(URI.create("/api/coupons/" + response.getId()))
                .body(ApiResponse.ok(response));
    }

    // 쿠폰 논리적 삭제
    @DeleteMapping("/{couponId}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long couponId) {
        couponService.deleteCoupon(couponId);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 지정한 쿠폰 정책으로 여러 개의 쿠폰 코드를 발행한다.
    @PostMapping("/{couponId}/codes")
    public ResponseEntity<ApiResponse<CouponCodeIssueResponse>> issueCodes(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponCodeIssueRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        couponService.issueCodes(couponId, request.getCount())
                ));
    }

    // 현재 로그인한 사용자에게 쿠폰 코드를 등록한다.
    /*
    @PostMapping("/issuance")
    public ResponseEntity<ApiResponse<CouponUserResponse>> registerCoupon(
            @Valid @RequestBody CouponRegisterRequest request,
            Authentication authentication
    ) {
        // 클라이언트가 보낸 userId가 아닌 인증된 사용자를 사용한다.
        User user = authUserStil.getAuthUser(authentication);

        return ResponseEntity.ok(
                ApiResponse.ok(couponUserService.registerCoupon(request.getCouponCode(), user))
        );
    }
    */
}
