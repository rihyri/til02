package io.github.rihyri.til02.week09.day02.example.ecommerce.order.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 *  애그리거트 내부 엔티티 : 주문 상품 한 줄
 *  외부에서 직접 생성/수정하지 않고 반드시 루트(Order)를 통해서만 다룬다.
 *  상품 정보는 Product 컨텍스트의 엔티티를 참조하지 않고, 주문 시점의 값(ID, 이름, 가격)을 복사해 둔다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private String productName;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_price"))
    private Money unitPrice;

    private int quantity;

    OrderLine(Long productId, String productName, Money unitePrice, int quantity) {
        verifyQuantity(quantity);
        this.productId = productId;
        this.productName = productName;;
        this.unitPrice = unitePrice;
        this.quantity = quantity;
    }

    // package-private : 같은 패키지의 루트(Order)만 호출할 수 있다.
    void changeQuantity(int quantity) {
        verifyQuantity(quantity);
        this.quantity = quantity;
    }

    private static void verifyQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다.");
        }
    }

    public Money amount() {
        return unitPrice.times(quantity);
    }
}
