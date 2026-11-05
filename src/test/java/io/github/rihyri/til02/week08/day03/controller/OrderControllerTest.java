package io.github.rihyri.til02.week08.day03.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.rihyri.til02.week08.day03.example.entity.Product;
import io.github.rihyri.til02.week08.day03.example.entity.User;
import io.github.rihyri.til02.week08.day03.example.repository.OrderRepository;
import io.github.rihyri.til02.week08.day03.example.repository.ProductRepository;
import io.github.rihyri.til02.week08.day03.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {

        // API 테스트에 사용할 데이터 준비
        testUser = userRepository.save(
                new User("테스트사용자", "test@example.com")
        );

        testProduct = productRepository.save(
                new Product(
                        "노트북",
                        new BigDecimal("1000000"),
                        10
                )
        );
    }

    @Test
    @DisplayName("정상적인 주문 요청은 HTTP 200을 반환한다.")
    void testCreateOrder() throws Exception {

        // Given
        String requestBody = """
                {
                    "userId": %d,
                    "product": [
                        {
                            "productId": %d,
                            "quantity": 2
                        }
                    ]
                }
                """.formatted(testUser.getId(), testProduct.getId());

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.totalPrice").value(2000000))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // 실제 DB의 재고 변경도 검증
        Product product = productRepository
                .findById(testProduct.getId())
                .orElseThrow();

        assertThat(product.getStock()).isEqualTo(8);
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("재고가 부족하면 HTTP 400을 반환한다.")
    void testCreateOrder_whenStockIsInsufficient() throws Exception {

        // Given
        String requestBody = """
                {
                    "userId": %d,
                    "product:" [
                        {
                            "productId": %d,
                            "quantity": 20
                        }
                    ]
                }
                """.formatted(testUser.getId(), testProduct.getId());

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.message").value("재고가 부족합니다."));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 상품을 요청하면 HTTP 400을 반환한다.")
    void testCreateOrder_whenProductNotFound() throws Exception {

        // Given
        String requestBody = """
                {
                    "userId": %d,
                    "product": [
                        {
                            "productId": 99999,
                            "quantity": 1
                        }
                    ]
                }
                """.formatted(testUser.getId());

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다."));

        assertThat(orderRepository.count()).isZero();
    }
}
