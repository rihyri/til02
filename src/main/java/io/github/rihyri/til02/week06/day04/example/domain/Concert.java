package io.github.rihyri.til02.week06.day04.example.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "concerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "remaining_seats", nullable = false)
    private Integer remainingSeats;

    // 낙관적 락에서 데이터 변경 여부를 확인할 때 사용하는 버전
    @Version
    @Column(nullable = false)
    private Long version;

    // 예약 요청 수량만큼 잔여 좌석을 감소시킨다.
    public void reserve(int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException("예약 수량은 1 이상이어야 합니다.");
        }

        if (remainingSeats < quantity) {
            throw new IllegalStateException("잔여 좌석이 부족합니다.");
        }

        this.remainingSeats -= quantity;
    }
}
