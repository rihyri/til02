package io.github.rihyri.til02.week07.day03.example.repository;

import io.github.rihyri.til02.week07.day03.example.dto.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.math.BigDecimal;
import java.sql.PreparedStatement;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final JdbcTemplate jdbc;

    // 주문 생성
    public long save(long userId, long taskQueueId) {

        var key = new GeneratedKeyHolder();

        jdbc.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO orders(user_id, task_queue_id, total_price) "
                            + "VALUES (?, ?, 0)",
                    PreparedStatement.RETURN_GENERATED_KEYS
            );

            ps.setLong(1, userId);
            ps.setLong(2, taskQueueId);

            return ps;
        }, key);

        return key.getKey().longValue();
    }

    // 주문 상품 등록 및 총액 계산
    public BigDecimal saveItemsAndCalculateTotal(long orderId, List<OrderRequest.ProductRequest> products) {

        BigDecimal total = BigDecimal.ZERO;

        for (var product : products) {

            jdbc.update(
                    "INSERT INTO order_item"
                                + "(order_id, product_name, unit_price, quantity) "
                                + "VALUES (?, ?, ?, ?)",
                    orderId,
                    product.name(),
                    product.price(),
                    product.quantity()
            );

            total = total.add(
                    product.price().multiply(
                            BigDecimal.valueOf(product.quantity())
                    )
            );
        }

        return total;
    }

    // 주문 총액 수정
    public void updateTotal(long orderId, BigDecimal total) {

        jdbc.update(
                "UPDATE orders SET total_price = ? WHERE id = ?",
                total,
                orderId
        );
    }
}
