package io.github.rihyri.til02.week07.day03.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Statement;
import java.util.function.LongConsumer;

@Service
@RequiredArgsConstructor
public class TaskQueueService {

    private final JdbcTemplate jdbc;

    // 작업 큐 등록
    @Transactional(propagation = Propagation.REQUIRED)
    public long requestQueue() {

        var key = new GeneratedKeyHolder();

        jdbc.update(connection -> connection.prepareStatement(
                "INSERT INTO task_queue(status) VALUES ('WAITING')",
                Statement.RETURN_GENERATED_KEYS
        ), key);

        return key.getKey().longValue();
    }

    // 등록된 작업 큐 처리
    @Transactional(propagation = Propagation.REQUIRED)
    public void processQueueById(long taskQueueId, LongConsumer task) {

        // 다른 트랜잭션이 같은 작업을 수정하지 못하도록 작업
        String status = jdbc.queryForObject(
                "SELECT status FROM task_queue WHERE id = ? FOR UPDATE",
                String.class,
                taskQueueId
        );

        if (!"WAITING".equals(status)) {
            throw new IllegalStateException("이미 처리 중인 작업입니다.");
        }

        // 처리 중으로 상태 변경
        jdbc.update(
                "UPDATE task_queue SET status = 'PROCESSING' WHERE id = ?",
                taskQueueId
        );

        // 전달받은 주문 처리 로직 실행
        task.accept(taskQueueId);

        // 처리 완료로 상태 변경
        jdbc.update(
                "UPDATE task_queue SET status = 'COMPLETED' WHERE id = ?",
                taskQueueId
        );
    }

    // 작업 큐에 주문 ID 등록
    public void setEventId(long queueId, long orderId) {

        jdbc.update(
                "UPDATE task_queue SET event_id = ? WHERE id = ?",
                orderId,
                queueId
        );
    }
}
