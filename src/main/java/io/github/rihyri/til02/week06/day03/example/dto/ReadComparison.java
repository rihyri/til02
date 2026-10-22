package io.github.rihyri.til02.week06.day03.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReadComparison {

    // 같은 트랜잭션에서 처음 조회한 결과
    private final long firstValue;

    // 다른 트랜잭션의 작업 이후 다시 조회한 결과
    private final long secondValue;

    // 조회 결과가 달라졌는지 확인한다.
    public boolean isChanged() {
        return firstValue != secondValue;
    }
}
