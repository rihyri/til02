package io.github.rihyri.til02.week07.day03.example.service;

import io.github.rihyri.til02.week07.day03.example.dto.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AsyncOrderService {

    private final OrderProcessService orderProcessService;

    @Async
    @Transactional(propagation = Propagation.REQUIRED)
    public void orderProcess(long taskQueueId, OrderRequest request) {

        try {
            // 별도 스레드에서 주문 처리
            orderProcessService.orderProcessRequired(
                    taskQueueId,
                    request,
                    false
            );
        } catch (RuntimeException e) {
            System.err.println("비동기 주문 실패: " + e.getMessage());

            throw e;
        }
    }
}
