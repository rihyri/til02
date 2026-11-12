package io.github.rihyri.til02.week09.day02.example.ecommerce.order.application.command;

import javax.sound.sampled.Line;
import java.util.List;

// 응용 서비스의 입력, presentation 계층의 HTTP DTO와 분리해 웹 기술에 대한 의존을 끊는다.
public record PlaceOrderCommand (Long userId, List<Line> lines, String zipCode, String address1, String address2) {

    public record Line(Long productId, int quantity) {
    }
}