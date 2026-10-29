package io.github.rihyri.til02.week07.day03.example.service;

import io.github.rihyri.til02.week07.day03.example.dto.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final TaskQueueService taskQueueService;
    private final OrderProcessService orderProcessService;
    private final AuditService auditService;
    private final OrderInfoService orderInfoService;
    private final AsyncOrderService asyncOrderService;

    // 1. REQUIRED: 부모와 자식이 하나의 트랜잭션 공유
    @Transactional(propagation = Propagation.REQUIRED)
    public void required(OrderRequest request) {

        long queueId = taskQueueService.requestQueue();

        // 자식에서 예외가 발생하면 전체 롤백
        orderProcessService.orderProcessRequired(
                queueId,
                request,
                true
        );
    }

    // 2. REQUIRES_NEW: 로그는 별도 트랜잭션에 저장
    @Transactional(propagation = Propagation.REQUIRED)
    public void requiresNew(OrderRequest request) {

        taskQueueService.requestQueue();

        // 부모 트랜잭션과 별도로 로그 저장
        auditService.saveLog("주문 요청을 받았습니다.");

        // 부모에서만 예외 발생
        throw new IllegalStateException("부모 트랜잭션의 의도적인 오류");
    }

    // 3. NESTED: 자식 작업만 부분 롤백
    @Transactional(propagation = Propagation.NESTED)
    public void nested(OrderRequest request) {

        long queueId = taskQueueService.requestQueue();

        try {
            orderProcessService.orderProcessNested(
                    queueId,
                    request,
                    true
            );
        } catch (IllegalStateException e) {

            // 자식 롤백 후 부모는 계속 실행
            System.out.println("자식 오류 처리: " + e.getMessage());
        }
    }

    // 4. SUPPORTS: 부모 트랜잭션 안에서 조회
    @Transactional(propagation = Propagation.SUPPORTS)
    public String supportsInside(long userId) {

        return orderInfoService.getUser(userId);
    }

    // 5. 비동기 주문 요청 접수
    @Transactional(propagation = Propagation.REQUIRED)
    public long orderRequestAsync(OrderRequest request) {

        long queueId = taskQueueService.requestQueue();

        // 작업 큐가 커밋된 후 비동기 주문 처리 시작
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        asyncOrderService.orderProcess(
                                queueId,
                                request
                        );
                    }
                }
        );

        return queueId;
    }
}
