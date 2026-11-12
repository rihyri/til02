package io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model;

import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.event.OrderCancelledEvent;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.event.OrderCompletedEvent;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.event.OrderPlacedEvent;
import io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.service.ShippingFeePolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 *  애그 리거트 루트: 주문
 *
 *  규칙(불변식, invariant)은 모두 이 클래스 안에서 지킨다.
 *  1) 주문 상품은 1개 이상이어야 한다.
 *  2) 총 금액 = Σ(단가 × 수량) + 배송비
 *  3) PENDING 상태에서만 배송지 변경 / 수량 변경 / 취소 / 확정이 가능하다.
 *
 *  setter를 열지 않고 place(), cancel(), complete()처럼 "비즈니스 의도가 드러나는 메서드"로만 상태를 바꾼다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class Order extends AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 다른 애그리거트(User)는 객체가 아닌 ID로만 참조한다.
    private Long userId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderLine> lines = new ArrayList<>();

    @Embedded
    private ShippingAddress shippingAddress;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "shpping_fee"))
    private Money shippingFee;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_amount"))
    private Money totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String cancelReason;
    private LocalDateTime orderedAt;

    // 정적 팩토리: 올바른 상태의 주문만 만들어지도록 생성 규칙을 한 곳에 모은다.
    public static Order place(Long userId, List<OrderLineSpec> specs, ShippingAddress address, Money shippingFee) {

        if (userId == null) {
            throw new IllegalArgumentException("주문자 ID는 필수입니다.");
        }

        if (specs == null || specs.isEmpty()) {
            throw new IllegalArgumentException("주문 상품은 1개 이상이어야 합니다.");
        }

        Order order = new Order();
        order.userId = userId;
        order.shippingAddress = address;
        specs.forEach(spec -> order.lines.add(
                new OrderLine(spec.productId(), spec.productName(), spec.unitPrice(), spec.quantity())));
        order.shippingFee = shippingFee;
        order.totalAmount = order.linesAmount().plus(shippingFee);
        order.status = OrderStatus.PENDING;
        order.orderedAt = LocalDateTime.now();
        return order;
    }

    // 저장으로 ID가 생긴 직후 호출되어 "주문이 접수됨" 이벤트를 기록한다.
    public void markPlaced() {
        registerEvent(new OrderPlacedEvent(id, userId, totalAmount.amount(), LocalDateTime.now()));
    }

    public Money linesAmount() {
        return lines.stream().map(OrderLine::amount).reduce(Money.ZERO, Money::plus);
    }

    public void changeShippingAddress(ShippingAddress newAddress) {
        verifyModifiable();
        this.shippingAddress = newAddress;
    }

    /*
     *  주문 상품의 수량을 바꾼다. 수량이 바뀌면 상품 금액 한계가 달라지므로
     *  배송비와 총액도 루트가 함께 다시 계산해 불변식(총액 = 상품 금액 합계 + 배송비)을 지킨다.
     */
    public void changeQuantity(Long productId, int quantity, ShippingFeePolicy shippingFeePolicy) {
        verifyModifiable();
        OrderLine line = lines.stream()
                .filter(l -> l.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("주문에 없는 상품입니다. productId =" + productId));
        line.changeQuantity(quantity);
        this.shippingFee = shippingFeePolicy.calculate(linesAmount());
        this.totalAmount = linesAmount().plus(shippingFee);
    }

    public void complete() {
        verifyModifiable();
        this.status = OrderStatus.COMPLETED;
        registerEvent(new OrderCompletedEvent(id, LocalDateTime.now()));
    }

    public void cancel(String reason) {
        verifyModifiable();
        this.status = OrderStatus.CANCELED;
        this.cancelReason = reason;
        registerEvent(new OrderCancelledEvent(id, reason, LocalDateTime.now()));
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    private void verifyModifiable() {
        if (!status.isModifiable()) {
            throw new IllegalArgumentException("이미 " + status + " 상태인 주문은 변경할 수 없습니다. orderId = " + id);
        }
    }

    // 주문 생성에 필요한 상품 한 줄의 명세 (도메인 계층의 입력 값)
    public record OrderLineSpec(Long productId, String productName, Money unitPrice, int quantity) {
    }
}
