package io.github.rihyri.til02.week07.day03.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final JdbcTemplate jdbc;

    // 부모와 독립적인 새 트랜잭션 시작
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(String message) {

        jdbc.update(
                "INSERT INTO audit_log(message) VALUES (?)",
                message
        );
    }
}
