package io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.controller;

import io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.domain.Payment;
import io.github.rihyri.til02.week09_example.kafka_order.payment_service.src.main.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 *  결제 내용 조회용 API (확인용)
 *
 *  GET  /payments
 *  결제를 "요청"하는 API는 없다. 결제는 Kafka 메시지로만 시작된다.
 */

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/payments")
    public List<Payment> getAll() {
        return paymentService.getAllPayments();
    }
}
