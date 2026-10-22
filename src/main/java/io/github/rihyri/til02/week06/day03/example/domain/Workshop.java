package io.github.rihyri.til02.week06.day03.example.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workshops")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workshop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    // 트랜잭션 격리 수준 실습에서 조회 및 수정할 데이터
    @Column(nullable = false)
    private Integer enrolledCount;

    @Builder
    public Workshop(String title, Integer enrolledCount) {
        this.title = title;
        this.enrolledCount = enrolledCount;
    }

    public void changeEnrolledCount(int count) {
        this.enrolledCount = count;
    }
}
