package io.github.rihyri.til02.week06.project.coupon.entity;

import io.github.rihyri.til02.global.exception.DomainException;
import io.github.rihyri.til02.global.exception.DomainExceptionCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;

import java.time.LocalDateTime;

@Table(name = "coupon_user")
@Entity
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // 여러 쿠폰 코드가 하나의 쿠폰 정책을 참조한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    // 같은 쿠폰 코드가 DB에 중복 저장되는 것을 방지한다.
    @Column(nullable = false, unique = true, length = 30)
    String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    CouponUserStatus status;

    LocalDateTime registeredAt;

    LocalDateTime usedAt;

    @Builder
    public CouponUser(Coupon coupon, String code) {
        this.coupon = coupon;
        this.code = code;

        // 최초 발행 시에는 아직 사용자에게 등록되지 않은 상태
        this.status = CouponUserStatus.CREATED;
    }

    // 사용자에게 쿠폰을 등록한다.
    public void register(User user) {

        // CREATED가 아니라면 이미 등록되었거나 사용된 쿠폰이므로 거절
        if (this.status != CouponUserStatus.CREATED) {
            throw new DomainException(
                    DomainExceptionCode.ALREADY_REGISTERED_COUPON
            );
        }

        // 쿠폰 소유자를 지정하고 상태를 변경한다.
        this.user = user;
        this.status = CouponUserStatus.ISSUED;
        this.registeredAt = LocalDateTime.now();
    }
}
