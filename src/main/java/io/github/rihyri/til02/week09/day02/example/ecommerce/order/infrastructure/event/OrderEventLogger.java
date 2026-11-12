package io.github.rihyri.til02.week09.day02.example.ecommerce.order.infrastructure.event;

import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.event.OrderCancelledEvent;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.event.OrderCompletedEvent;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.event.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/*
 *  도메인 이벤트 핸들러
 *
 *  트랜잭션이 "커밋된 뒤"에만 실행된다.
 */
@Slf4j
@Component
public class OrderEventLogger {

    @TransactionalEventListener
    public void on(OrderPlacedEvent event) {
        log.info("[도메인 이벤트] 주문 접수됨: {}", event);
    }

    @TransactionalEventListener
    public void on(OrderCompletedEvent event) {
        log.info("[도메인 이벤트] 주문 확정됨: {}", event);
    }

    @TransactionalEventListener
    public void on(OrderCancelledEvent event) {
        log.info("[도메인 이벤트] 주문 취소됨: {}", event);
    }
}
