package io.github.rihyri.til02.week09.day02.example.ecommerce.order.application;

import io.github.rihyri.til02.week09.day02.example.ecommerce.order.application.command.PlaceOrderCommand;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.application.port.ProductCatalog;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model.Money;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model.Order;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model.Order.OrderLineSpec;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model.ShippingAddress;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.repository.OrderRepository;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.service.ShippingFeePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 *  응용 서비스 (Application Service): 유스케이스의 "흐름"만 조율한다.
 *  - 트랜잭션 경계 설정
 *  - 리포지터리로 애그리거트 조회/저장
 *  - 외부 포트 호출
 *  - 도메인 이벤트 발행
 *  비즈니스 규칙(금액 계산, 상태 전이 검증)은 직접 구현하지 않고 도메인 객체에 위임한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductCatalog productCatalog;
    private final ApplicationEventPublisher eventPublisher;
    private final ShippingFeePolicy shippingFeePolicy = new ShippingFeePolicy();

    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        List<OrderLineSpec> specs = command.lines().stream()
                .map(line -> {
                    ProductCatalog.ProductInfo product = productCatalog.findById(line.productId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. productId=" + line.productId()));
                    return new OrderLineSpec(product.productId(), product.name(), new Money(product.price()), line.quantity());
                })
                .toList();

        Money linesAmount = specs.stream()
                .map(spec -> spec.unitPrice().times(spec.quantity()))
                .reduce(Money.ZERO, Money::plus);
        Money shippingFee = shippingFeePolicy.calculate(linesAmount);
        ShippingAddress address = new ShippingAddress(command.zipCode(), command.address1(), command.address2());

        Order order = orderRepository.save(Order.place(command.userId(), specs, address, shippingFee));
        order.markPlaced();
        publishEvents(order);
        return order;
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public Order changeShippingAddress(Long orderId, String zipCode, String address1, String address2) {
        Order order = getOrder(orderId);
        order.changeShippingAddress(new ShippingAddress(zipCode, address1, address2));
        return order;
    }

    @Transactional
    public Order changeQuantity(Long orderId, Long productId, int quantity) {
        Order order = getOrder(orderId);
        order.changeQuantity(productId, quantity, shippingFeePolicy);
        return order;
    }

    @Transactional
    public Order completeOrder(Long orderId) {
        Order order = getOrder(orderId);
        order.complete();
        publishEvents(order);
        return order;
    }

    @Transactional
    public Order cancelOrder(Long orderId, String reason) {
        Order order = getOrder(orderId);
        order.cancel(reason);
        publishEvents(order);
        return order;
    }

    private void publishEvents(Order order) {
        order.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
