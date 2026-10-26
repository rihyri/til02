package io.github.rihyri.til02.week06.project.coupon.service;

import io.github.rihyri.til02.global.exception.DomainException;
import io.github.rihyri.til02.global.exception.DomainExceptionCode;
import io.github.rihyri.til02.week06.project.coupon.dto.response.CouponUserResponse;
import io.github.rihyri.til02.week06.project.coupon.entity.CouponUser;
import io.github.rihyri.til02.week06.project.coupon.repository.CouponUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponUserService {

    private final CouponUserRepository couponUserRepository;

    /*
     *  발행된 쿠폰 코드를 사용자에게 등록한다.
     *
     *  비관적 락을 사용해 동일 코드에 대한 동시 등록을 방지한다.
     *  조회, 유효성 검증, 상태 변경을 하나의 트랜잭션으로 처리한다.
     */
    @Transactional
    public CouponUserResponse registerCoupon(String couponCode, SecurityProperties.User user) {

        // 공백 제거와 대문자 변환으로 입력 코드를 정규화한다.
        String code = couponCode.trim().toUpperCase();

        /*
         *  1. 쿠폰 코드를 조회하면서 비관적 쓰기 락을 획득한다.
         *  다른 트랜잭션이 같은 코드에 대한 쓰기 락을 요구하면 대기한다.
         */
        CouponUser couponUser = couponUserRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.NOT_FOUND_COUPON_CODE));

        // 2. 쿠폰 정책이 등록 가능한 상태인지 확인한다.
        if (!couponUser.getCoupon().isRegisterable(LocalDateTime.now())) {
            throw new DomainException(DomainExceptionCode.UNAVAILABLE_COUPON);
        }

        /*
         *  3. CREATED 상태인지 검사한 후 소유자와 상태를 변경한다.
         *
         *  같은 트랜잭션에서 락을 유지하므로 동시 등록을 방지할 수 있다.
         */
        couponUser.register(user);

        // 4. 변경된 쿠폰 정보를 응답 DTO로 변환한다.
        return CouponUserResponse.from(couponUser);
    }
}
