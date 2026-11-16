package io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.service;

import io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.domain.Order;
import io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.domain.OrderRepository;
import io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.domain.OrderStatus;
import io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.event.OrderCreatedEvent;
import io.github.rihyri.til02.week09_example.kafka_order.order_service.src.main.order.kafka.OrderEventProducer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Counter;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/*
 *  주문 비즈니스 로직
 *
 *  Kafka 코드(보내기/받기)는 kafka 패키지에 따로 두고,
 *  여기서는 "주문을 어떻게 처리할지"만 다룬다.
 */
@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    //  ⭐ 모니터리용 카운터 (숫자가 계속 올라가기만 하는 지표)
    //  Prometheus에서는 orders_created_total 같은 이름을 보인다.
    private final Counter createdCounter;
    private final Counter paidCounter;
    private final Counter cancelledCounter;

    public OrderService(OrderRepository orderRepository,
                         OrderEventProducer orderEventProducer,
                         MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
        this.createdCounter = meterRegistry.counter("orders.created");
        this.paidCounter = meterRegistry.counter("orders.paid");
        this.cancelledCounter = meterRegistry.counter("orders.cancelled");
    }

    /*
     *  ② 주문 생성: DB에 PENDING으로 저장 → Kafka에 이벤트 발행
     */
    @Transactional
    public Order createOrder(String productName, int quantity, long price) {
        Order order = orderRepository.save(Order.create(productName, quantity, price));
        log.info("[주문 생성] orderId = {}, 금액 = {}, 상태 = {}", order.getId(), order.getTotalAmount(), order.getStatus());

        // 결제 서비스에 직접 HTTP 요청을 보내지 않고, Kafka에 "주문 생성됨" 편지를 넣는다.
        // 결제 서비스가 꺼져 있어도 편지는 Kafka에 남아 있다가, 켜지면 처리된다.
        orderEventProducer.sendOrderCreated(
                new OrderCreatedEvent(order.getId(), productName, order.getTotalAmount()));

        // 참고: DB 저장과 Kafka 발행은 하나의 트랜잭션으로 묶이지 않는다.
        // 실무에서는 Outbox 패턴으로 보완한다.
        createdCounter.increment();
        return order;
    }

    /*
     *  ⑥ - 1 결제 성공 이벤트를 받았을 때 → PAID
     */
    @Transactional
    public void completeOrder(Long orderId) {
        Order order = getOrder(orderId);

        // 멱등성 처리: Kafka는 같은 메시지가 두 번 올 수도 있다.
        // 이미 처리된 주문이면 무시해서, 두 번 와도 결과가 같게 만든다.
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("[중복/무시] orderId = {}는 이미 {} 상태", orderId, order.getStatus());
            return;
        }

        order.markPaid();   // @Transactional 덕분에 save() 없이도 DB에 반영
        paidCounter.increment();
        log.info("[주문 완료] orderId = {} → PAID", orderId);
    }

    /*
     *  ⑥ - 2 결제 실패 이벤트를 받았을 때 → CANCELLED (보상 트랜잭션)
     *
     *  주문은 이미 DB에 저장(커밋)됐기 때무에 롤백할 수 없다.
     *  그래서 "취소"라는 반대 동작으로 앞의 작업을 되돌린다.
     */
    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        Order order = getOrder(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("[중복/무시] orderId = {} 는 이미 {} 상태", orderId, order.getStatus());
            return;
        }

        order.cancel(reason);
        cancelledCounter.increment();
        log.info("[주문 취소 - 보상] orderId = {} → CANCELLED, 사유 = {}", orderId, reason);
    }


    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 없습니다. id =" + orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
