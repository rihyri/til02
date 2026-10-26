package io.github.rihyri.til02.week06.project.coupon.service;

import io.github.rihyri.til02.global.exception.DomainException;
import io.github.rihyri.til02.global.exception.DomainExceptionCode;
import io.github.rihyri.til02.week06.project.coupon.dto.request.CouponCreateRequest;
import io.github.rihyri.til02.week06.project.coupon.dto.response.CouponCodeIssueResponse;
import io.github.rihyri.til02.week06.project.coupon.dto.response.CouponResponse;
import io.github.rihyri.til02.week06.project.coupon.dto.response.PageResponse;
import io.github.rihyri.til02.week06.project.coupon.entity.Coupon;
import io.github.rihyri.til02.week06.project.coupon.entity.CouponUser;
import io.github.rihyri.til02.week06.project.coupon.repository.CouponRepository;
import io.github.rihyri.til02.week06.project.coupon.repository.CouponUserRepository;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUserRepository couponUserRepository;

    // 혼동하기 쉬운 문자를 제외해 쿠폰 코드를 생성한다.
    private static final char[] CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();

    private static final int CODE_LENGTH = 12;

    // 쿠폰 코드처럼 예측하기 어려운 난수가 필요한 경우 사용
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional(readOnly = true)
    public CouponResponse getCoupon(Long couponId) {

        Coupon coupon = couponRepository
                .findByIdAndIsDeletedFalse(couponId)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.NOT_FOUND_COUPON));

        return CouponResponse.from(coupon);
    }

    @Transactional(readOnly = true)
    public PageResponse<CouponResponse> getCoupons(Boolean isActive, int page, int size) {

        // 최신 쿠폰이 먼저 조회되도록 생성일 내림차순 정렬
        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<CouponResponse> result = couponRepository
                .searchCoupons(isActive, LocalDateTime.now(), pageable)
                .map(CouponResponse::from);

        return PageResponse.from(result);
    }

    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {

        Coupon saved = couponRepository.save(request.toEntity());
        return CouponResponse.from(saved);
    }

    @Transactional
    public void deleteCoupon(Long couponId) {

        Coupon coupon = couponRepository
                .findByIdAndIsDeletedFalse(couponId)
                .orElseThrow(() -> new DomainException(
                        DomainExceptionCode.NOT_FOUND_COUPON
                ));

        // 영속 엔티티의 상태만 변경한다.
        coupon.delete();
    }

    /*
     *  쿠폰 코드 대량 발생
     *
     *  발행 수량 증가와 개별 코드 저장을 하나의 트랜잭션으로 묵는다.
     *
     *  두 작업 중 하나라도 실패하면 전체 작업을 롤백해
     *  coupon.issueCount와 실제 coupon_user 개수의 정합성을 보장한다.
     */
    @Transactional
    public CouponCodeIssueResponse issueCodes(Long couponId, int count) {

        Coupon coupon = couponRepository.findByIdAndIsDeletedFalse(couponId)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.NOT_FOUND_COUPON));

        // 종료된 쿠폰이라면 발행하지 않는다.
        if (LocalDateTime.now().isAfter(coupon.getEndDate())) {
            throw new DomainException(DomainExceptionCode.UNAVAILABLE_COUPON);
        }

        /*
         *  1. DB에서 발행 한도를 확인하면서 수량을 증가시킨다.
         *
         *  조건부 UPDATE이므로 동시 요청이 들어와도 한도 초과를 방지한다.
         */
        int updated = couponRepository.increaseIssueCount(couponId, count);

        // UPDATE된 행이 없다면 발행 한도 초과 등의 이유로 처리 불가
        if (updated == 0) {
            throw new DomainException(DomainExceptionCode.EXCEED_COUPON_USAGE_LIMIT);
        }

        // 2. 요청한 개수만큼 중복 없는 쿠폰 코드를 생성한다.
        Set<String> codes = generatedUniqueCodes(count);

        // 각 코드를 CREATED 상태의 CouponUser 엔티티로 변환한다.
        List<CouponUser> couponUsers = codes.stream()
                .map(code -> CouponUser.builder()
                        .coupon(coupon)
                        .code(code)
                        .build()
                ).toList();

        /*
         *  3. 생성한 쿠폰 코드를 DB에 저장한다.
         *  같은 트랜잭션 안이므로 저장 실패 시 수량 증가도 롤백된다.
         */
        couponUserRepository.saveAll(couponUsers);

        return CouponCodeIssueResponse.of(couponId, count, List.copyOf(codes));
    }

    /*
     *  DB에 존재하지 않는 고유한 쿠폰 코드를 생성한다.
     *
     *  Set으로 이번 요청 내부의 중복을 제거하고, Repository를 통해 기존 DB 데이터와의 중복도 검사한다.
     */
    private Set<String> generatedUniqueCodes(int count) {

        Set<String> codes = new HashSet<>();

        // Set은 중복 값을 추가하지 않으므로 count개가 될 때까지 반복
        while (codes.size() < count) {
            codes.add(generateCode());
        }

        // 생성된 코드 중 DB에 이미 존재하는 코드를 조회
        List<String> duplicated = couponUserRepository.findExistingCodes(codes);

        // DB에 중복된 코드가 있다면 제거하고 다시 생성한다.
        while (!duplicated.isEmpty()) {

            duplicated.forEach(codes::remove);

            while (codes.size() < count) {
                codes.add(generateCode());
            }

            // 새로 생성된 코드까지 포함해 다시 중복 검사
            duplicated = couponUserRepository.findExistingCodes(codes);
        }

        return codes;
    }

    // 12자리 코드를 4자리 단위로 나누어 생성한다.
    private String generateCode() {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {

            if (i > 0 && i % 4 == 0) {
                sb.append('-');
            }

            sb.append(
                    CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]
            );
        }

        return sb.toString();
    }
}
