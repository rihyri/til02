package io.github.rihyri.til02.week06.day01.example.transfer;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TransferOperations {

    // 잘못된 이체 요청인지 확인한다.
    public static void validate(TransferRequest request) {

        if (request.getFromId() == request.getToId()) {
            throw new IllegalArgumentException("출금 계좌와 입금 계좌는 달라야 합니다.");
        }
    }

    // 롤백 확인을 위해 의도적으로 예외를 발생시킨다.
    public static void failIfRequested(TransferRequest request) {

        if (request.isFailAfterWithdrawal()) {
            throw new IllegalArgumentException("트랜잭션 롤백 실험용 예외");
        }
    }
}
