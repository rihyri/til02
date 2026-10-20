package io.github.rihyri.til02.week06.day01.example.account;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    // 계좌 ID로 계좌 정보를 조회한다.
    public Account findById(long id) {

        return jdbcTemplate.query(
                "SELECT id, owner, balance FROM accounts WHERE id = ?",

                // ResultSet에서 조회된 데이터를 Account 객체로 변환한다.
                rs -> {
                    if (!rs.next()) {
                        throw new IllegalArgumentException(
                                "계좌를 찾을 수 없습니다: " + id
                        );
                    }

                    return new Account(
                            rs.getLong("id"),
                            rs.getString("owner"),
                            rs.getLong("balance")
                    );
                },

                id
        );
    }

    // 계좌 잔액을 증가시키거나 감소시킨다.
    public void changeBalance(long id, long delta) {

        int updated = jdbcTemplate.update(
                """
                    UPDATE accounts
                    SET balance = balance+ ?
                    WHERE id = ?
                        AND balance + ? >= 0    
                    """,
                    delta,
                    id,
                    delta
        );

        // 업데이트된 계좌가 없다면 예외 발생
        if (updated != 1) {
            throw new IllegalArgumentException(
                    "계좌가 없거나 잔액이 부족합니다.:" + id
            );
        }
    }
}
