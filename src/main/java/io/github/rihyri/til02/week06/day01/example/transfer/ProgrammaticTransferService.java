package io.github.rihyri.til02.week06.day01.example.transfer;

import io.github.rihyri.til02.week06.day01.example.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Service
@RequiredArgsConstructor
public class ProgrammaticTransferService {

    // 트랜잭션을 관리하기 위한 인터페이스이다. 트랜잭션의 시작, 커밋, 롤백 등의 기능을 즈ㅔ공한다.
    private final PlatformTransactionManager transactionManager;
    private final AccountRepository accountRepository;

    public void transfer(TransferRequest request) {

        TransferOperations.validate(request);

        // 트랜잭션의 속성과 동작 방식을 정의한다.
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

        // 트랜잭션을 시작하고 현재 상태를 반환받는다.
        TransactionStatus status = transactionManager.getTransaction(definition);

        try {
            // 1. 출금 처리
            accountRepository.changeBalance(request.getFromId(), -request.getAmount());

            // 2. 롤백을 확인하기 위한 테스트
            TransferOperations.failIfRequested(request);

            // 3. 입금 처리
            accountRepository.changeBalance(
                    request.getToId(), request.getAmount()
            );

            // 4. 작업이 성공하면 커밋한다.
            transactionManager.commit(status);
        } catch (RuntimeException | Error e) {

            // 트랜잭션이 아직 종료되지 않았다면 롤백한다.
            if (!status.isCompleted()) {
                transactionManager.rollback(status);
            }

            throw e;
        }
    }
}
