package io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.service;

import io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.domain.Payment;
import io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.domain.PaymentRepository;
import io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.event.OrderCreatedEvent;
import io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.event.PaymentResultEvent;
import io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.kafka.PaymentResultProducer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 결제 비즈니스 로직
@Slf4j
public class PaymentService {

    // 예제 규칙: 한 번에 결제할 수 있는 최대 금액 (이 금액을 넘으면 "잔액 부족"으로 실패)
    private static final long MAX_AMOUNT = 100_000;

    private final PaymentRepository paymentRepository;
    private final PaymentResultProducer paymentResultProducer;
    private final Counter successCounter;
    private final Counter failCounter;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentResultProducer paymentResultProducer,
                          MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.paymentResultProducer = paymentResultProducer;
        // Prometheus에서 payments_total{result="success"} / {result="fail"}로 보인다.
        this.successCounter = meterRegistry.counter("payments", "result", "success");
        this.failCounter = meterRegistry.counter("payments", "result", "fail");
    }

    // ⑤ 결제 처리 후 결과 발행
    @Transactional
    public void pay(OrderCreatedEvent event) {
        // 멱등성: 같은 주문 메시지가 두 번 와도 결제는 한 번만
        if (paymentRepository.existsByOrderId(event.orderId())) {
            log.warn("[중복/무시] orderI = {}는 이미 결제 처리됨", event.orderId());
            return;
        }

        if (event.totalAmount() > MAX_AMOUNT) {
            // ── 실패 경로 ──
            String reason = "잔액 부족 (껄제 한도 " + MAX_AMOUNT + "원 초과";
            Payment payment = paymentRepository.save(Payment.failed(event.orderId(), event.totalAmount(), reason));
            log.info("[결제 실패] orderId = {}, 금액 = {}", event.orderId(), event.totalAmount());

            // 결제 서비스는 주문을 직접 취소할 수 없다. (주문 DB에 접근 불가)
            // "실패했어요"라고 알리기만 하면, 주문 서비스가 알아서 취소(보상)한다.
            paymentResultProducer.sendFailed(new PaymentResultEvent(event.orderId(), payment.getId(), reason));

            failCounter.increment();
            return;
        }

        // ── 성공 경로 ──
        Payment payment = paymentRepository.save(Payment.completed(event.orderId(), event.totalAmount()));
        log.info("[결제 성공] orderId = {}, paymentId = {}", event.orderId(), payment.getId());
        paymentResultProducer.sendCompleted(new PaymentResultEvent(event.orderId(), payment.getId(), null));
        successCounter.increment();
    }

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}
