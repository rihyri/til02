package io.github.rihyri.til02.week07.day03.example.service;

import io.github.rihyri.til02.week07.day03.example.dto.OrderRequest;
import io.github.rihyri.til02.week07.day03.example.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderProcessService {

    private final TaskQueueService taskQueueService;
    private final OrderRepository orderRepository;

    // 기존 트랜잭션에 참여
    @Transactional(propagation = Propagation.REQUIRED)
    public void orderProcessRequired(long queueId, OrderRequest request, boolean fail) {

        process(queueId, request, fail);
    }

    // SAVEPOINT를 이용한 중첩 트랜잭션
    @Transactional(propagation = Propagation.NESTED)
    public void orderProcessNested(long queueId, OrderRequest request, boolean fail) {

        process(queueId, request, fail);
    }

    // 실제 주문 처리
    private  void process(long queueId, OrderRequest request, boolean fail) {

        taskQueueService.processQueueById(queueId, id -> {

            // 1. 주문 저장
            long orderId = orderRepository.save(
                    request.userId(),
                    id
            );

            // 2. 작업 큐에 주문 ID 연결
            taskQueueService.setEventId(id, orderId);

            // 3. 주문 상품 생성 및 총액 계산
            var total = orderRepository.saveItemsAndCalculateTotal(
                    orderId,
                    request.products()
            );

            // 4. 주문 총액 수정
            orderRepository.updateTotal(orderId, total);
        });

        // 롤백 결과를 확인하기 위한 의도적인 예외
        if (fail) {
            throw new IllegalStateException("주문 처리 중 의도적인 오류");
        }
    }
}
