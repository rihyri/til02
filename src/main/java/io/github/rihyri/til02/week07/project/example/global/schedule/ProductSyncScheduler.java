package io.github.rihyri.til02.week07.project.example.global.schedule;

import io.github.rihyri.til02.week07.project.example.domain.product.entity.SyncResult;
import io.github.rihyri.til02.week07.project.example.domain.product.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSyncScheduler {

    private final ProductSyncService productSyncService;

    @Scheduled(cron = "${external.external-shop.sync-cron}")
    public void syncExternalProducts() {

        LocalDateTime start = LocalDateTime.now();
        log.info("[상품 동기화] 시작: {}", start);

        try {
            SyncResult r = productSyncService.syncAll();
            log.info("[상품 동기화] 완료: 신규 {}건, 수정 {}건, 총 {}건, {}ms",
                    r.getCreated(), r.getUpdated(), r.getTotal(), Duration.between(start, LocalDateTime.now()).toMillis());
        } catch (Exception e) {
            log.error("[상품 동기화] 실패, 다음 주기에 다시 시도합니다", e);
        }
    }
}
