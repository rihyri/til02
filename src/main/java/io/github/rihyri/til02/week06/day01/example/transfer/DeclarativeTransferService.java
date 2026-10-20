package io.github.rihyri.til02.week06.day01.example.transfer;

import io.github.rihyri.til02.week06.day01.example.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeclarativeTransferService {

    private final AccountRepository accountRepository;

    // 메서드 실행 전후의 트랜잭션을 Spring이 관리한다.
    @Transactional
    public void transfer(TransferRequest request) {

        TransferOperations.validate(request);

        // 1. 출금 처리
        accountRepository.changeBalance(
                request.getFromId(), -request.getAmount()
        );

        // 2. 예외 발생 시 전체 작업 롤백
        TransferOperations.failIfRequested(request);

        // 3. 입금 처리
        accountRepository.changeBalance(
                request.getToId(), request.getAmount()
        );
    }
}
