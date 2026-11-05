package io.github.rihyri.til02.week08.day03.example.service;

import io.github.rihyri.til02.week08.day03.example.dto.OrderProductRequest;
import io.github.rihyri.til02.week08.day03.example.entity.Order;
import io.github.rihyri.til02.week08.day03.example.entity.OrderProduct;
import io.github.rihyri.til02.week08.day03.example.entity.Product;
import io.github.rihyri.til02.week08.day03.example.entity.User;
import io.github.rihyri.til02.week08.day03.example.repository.OrderProductRepository;
import io.github.rihyri.til02.week08.day03.example.repository.OrderRepository;
import io.github.rihyri.til02.week08.day03.example.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderProcessService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderProductRepository orderProductRepository;

    @Transactional
    public Order process(User user, List<OrderProductRequest> orderItems) {

        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 없습니다.");
        }

        Order order = new Order(user);
        List<OrderProduct> orderProducts = new ArrayList<>();

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderProductRequest item : orderItems) {

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("상품을 찾을 수 없습니다.")
                    );

            // 재고 확인 및 감소
            product.decreaseStock(item.getQuantity());

            BigDecimal itemPrice = product.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            totalPrice = totalPrice.add(itemPrice);

            orderProducts.add(
                    new OrderProduct(order, product, item.getQuantity())
            );
        }

        order.updateTotalPrice(totalPrice);

        // 주문 및 주문 상품 저장
        Order savedOrder = orderRepository.save(order);
        orderProductRepository.saveAll(orderProducts);

        return savedOrder;
    }
}
