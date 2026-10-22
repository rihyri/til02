package io.github.rihyri.til02.week06.day03.example.repository;

import io.github.rihyri.til02.week06.day03.example.domain.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopRepository extends JpaRepository<Workshop, Long> {

    // 신청 인원이 지정한 수보다 많은 강의의 개수를 조회한다.
    long countByEnrolledCountGreaterThan(int count);
}
