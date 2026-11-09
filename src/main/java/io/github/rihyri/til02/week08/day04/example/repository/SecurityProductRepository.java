package io.github.rihyri.til02.week08.day04.example.repository;

import io.github.rihyri.til02.week08.day04.example.dto.SecurityProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SecurityProductRepository {

    private final JdbcTemplate jdbcTemplate;

    // 클라이언트가 선택할 수 있는 정렬 기준
    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
        "name", "name",
        "price", "price",
        "createdAt", "createdAt"
    );

    private final RowMapper<SecurityProductResponse> productRowMapper =
            (rs, rowNum) -> new SecurityProductResponse(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getInt("price")
            );

    // SQL Injection 방지: 사용자 입력값을 ?로 바인딩
    public List<SecurityProductResponse> searchByName(String name) {

        String sql = """
                    SELECT id, name, price
                    FROM security_products
                    WHERE name = ?
                    """;

        return jdbcTemplate.query(sql, productRowMapper, name);
    }

    // ORDER BY는 허용된 컬럼만 사용할 수 있도록 검증
    public List<SecurityProductResponse> searchAll(String sort) {

        String column = SORTABLE_COLUMNS.get(sort);

        if (column == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "허용되지 않은 정렬 기준입니다.;"
            );
        }

        String sql = """
                SELECT id, name, price
                FROM security_products
                ORDER BY
                """ + column;

        return jdbcTemplate.query(sql, productRowMapper);
    }
}
