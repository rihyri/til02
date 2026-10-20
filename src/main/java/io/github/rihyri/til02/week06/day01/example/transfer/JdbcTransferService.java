package io.github.rihyri.til02.week06.day01.example.transfer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor
public class JdbcTransferService {

    private final DataSource dataSource;


    public void transfer(TransferRequest request) {

        TransferOperations.validate(request);

        // DataSource에서 DB Connection을 가져온다.
        try (Connection connection = dataSource.getConnection()) {

            // 자동 커밋을 해제하여 트랜잭션을 직접 관리한다.
            // 자동 커밋이 활성화된 상태에서는 각각 SQL 실행이 완료될 때 변경 사항이 커밋된다.
            connection.setAutoCommit(false);

            try {
                // 1. 출금 계좌의 잔액 감소
                changeBalance(
                        connection, request.getFromId(), -request.getAmount()
                );

                // 2. 예외를 발생시켜 롤백을 확인한다.
                TransferOperations.failIfRequested(request);

                // 3. 입금 계좌의 잔액 증가
                changeBalance(
                        connection, request.getToId(), request.getAmount()
                );

                // 4. 모든 작업이 성공하면 커밋한다.
                connection.commit();
            } catch (Exception e) {
                // 작업 중 오류가 발생하면 변경 사항을 취소한다.
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw new IllegalStateException("JDBC 이체 실패 (롤백 시도)", e);
            } finally {

                // Connection을 반환하기 전에 자동 커밋 설정을 복원한다.
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {

            throw new IllegalStateException("JDBC 연결 처리 실패", e);
        }
    }

    // 전달받은 Connection을 이용해 잔액을 변경한다.
    private void changeBalance(Connection connection, long id, long delta) throws SQLException {

        String sql = """
                UPDATE accounts
                SET balance = balance + ?
                WHERE id = ?
                    AND balance + ? >= 0
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, delta);
            statement.setLong(2, id);
            statement.setLong(3, delta);

            int updated = statement.executeUpdate();

            if (updated != 1) {
                throw new IllegalArgumentException("계좌가 없거나 잔액이 부족합니다: " + id);
            }
        }
    }
}
