package io.github.rihyri.til02.week08.day03.service;

import io.github.rihyri.til02.week08.day03.example.dto.OrderProductRequest;
import io.github.rihyri.til02.week08.day03.example.entity.Order;
import io.github.rihyri.til02.week08.day03.example.entity.OrderProduct;
import io.github.rihyri.til02.week08.day03.example.entity.Product;
import io.github.rihyri.til02.week08.day03.example.entity.User;
import io.github.rihyri.til02.week08.day03.example.repository.OrderProductRepository;
import io.github.rihyri.til02.week08.day03.example.repository.OrderRepository;
import io.github.rihyri.til02.week08.day03.example.repository.ProductRepository;
import io.github.rihyri.til02.week08.day03.example.service.OrderProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderProcessServiceTest {

    @InjectMocks
    private OrderProcessService orderProcessService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderProductRepository orderProductRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {

        testUser = new User("테스트사용자", "test@example.com");

        // DB 없이 테스트하므로 ID 직접 설정
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testProduct = new Product(
                "노트북",
                new BigDecimal("1000000"),
                10
        );

        ReflectionTestUtils.setField(testProduct, "id", 1L);
    }

    @Test
    @DisplayName("재고가 충분하면 주문이 성공하고 재고가 감소한다.")
    void process_shouldSucceed_whenProductInStock() {

        // Arrange: 테스트 준비
        OrderProductRequest orderItem = new OrderProductRequest(1L, 2);
        List<OrderProductRequest> orderItems = List.of(orderItem);

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: 주문 실행
        Order result = orderProcessService.process(testUser, orderItems);

        // Assert: 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("2000000"));
        assertThat(testProduct.getStock()).isEqualTo(8);

        verify(productRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
        verify(orderProductRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("재고가 부족하면 예외가 발생하고 주문이 저장되지 않는다.")
    void process_shouldFail_whenStockIsInsufficient() {

        // Given
        OrderProductRequest orderItem = new OrderProductRequest(1L, 11);

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When & Then
        assertThatThrownBy(() ->
                orderProcessService.process(
                        testUser,
                        List.of(orderItem)
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("재고가 부족합니다.");

        assertThat(testProduct.getStock()).isEqualTo(10);

        verify(orderRepository, never()).save(any());
        verify(orderProductRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("재고와 주문 수량이 같으면 주문이 성공한다.")
    void process_shouldSucceed_whenQuantityEqualsStock() {

        // Given
        OrderProductRequest orderItem = new OrderProductRequest(1L, 10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Order result = orderProcessService.process(
                testUser,
                List.of(orderItem)
        );

        // Then
        assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("10000000"));
        assertThat(testProduct.getStock()).isZero();

        verify(orderRepository).save(any(Order.class));
        verify(orderProductRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("주문 수량이 0이면 예외가 발생한다.")
    void process_shouldFail_whenQuantityIsZero() {

        // Given
        OrderProductRequest orderItem = new OrderProductRequest(1L, 0);

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When & Then
        assertThatThrownBy(() ->
                orderProcessService.process(
                        testUser,
                        List.of(orderItem)
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 1개 이상이어야 합니다.");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 상품을 주문하면 예외가 발생한다.")
    void process_shouldFail_whenProductNotFound() {

        // Given
        OrderProductRequest orderItem = new OrderProductRequest(99L, 2);

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                orderProcessService.process(
                        testUser,
                        List.of(orderItem)
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품을 찾을 수 없습니다.");

        verify(productRepository).findById(99L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("주문 상품 목록이 비어 있으면 예외가 발생한다.")
    void process_shouldFail_whenOrderItemsEmpty() {

        // When & Then
        assertThatThrownBy(() ->
                orderProcessService.process(testUser, List.of())
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주문 상품이 없습니다.");

        verifyNoInteractions(
                productRepository,
                orderRepository,
                orderProductRepository
        );
    }
}
