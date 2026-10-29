package io.github.rihyri.til02.week07.day03.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class OrderInfoService {

    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.SUPPORTS)
    public String getUser(long userId) {

        String name = jdbc.queryForObject(
                "SELECT name FROM users WHERE id = ?",
                String.class,
                userId
        );

        boolean txActive =
                TransactionSynchronizationManager.isActualTransactionActive();

        return "user=" + name + ", txActive=" + txActive;
    }
}
