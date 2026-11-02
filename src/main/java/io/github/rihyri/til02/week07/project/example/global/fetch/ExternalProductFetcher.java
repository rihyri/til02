package io.github.rihyri.til02.week07.project.example.global.fetch;

import feign.FeignException;
import feign.RetryableException;
import io.github.rihyri.til02.week07.project.example.global.exception.ExternalApiException;
import io.github.rihyri.til02.week07.project.example.global.external.client.ExternalShopClient;
import io.github.rihyri.til02.week07.project.example.global.external.dto.ExternalProductResponse;
import io.github.rihyri.til02.week07.project.example.global.external.dto.ExternalProductResponse.ExternalPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalProductFetcher {

    private final ExternalShopClient externalShopClient;

    @Retryable(
            retryFor = {
                    RetryableException.class,                   // 연결 실패, 타임아웃
                    FeignException.FeignClientException.class,  // 5xx
                    ExternalApiException.class                  // result=false (200으로 오는 실패)
            },
            maxAttempts = 4,                                    // 1회 + 재시도 3회
            backoff = @Backoff(delay = 1000, multiplier = 2)    // 1s → 2s → 4s
    )
    public ExternalPage fetchPage(int page, int size) {

        log.info("[외부 API] page = {} 호출 시도", page);
        ExternalProductResponse res = externalShopClient.getProducts(page, size);

        if (res == null || res.getMessage() == null) {
            String reason = (res != null && res.getError() != null) ? res.getError().getErrorMessage() : "empty response";

            throw new ExternalApiException("외부 상품 조회 실패(page=" + page + "): " + reason);
        }

        return res.getMessage();
    }

    @Recover
    public ExternalPage recover(RuntimeException e, int page, int size) {

        log.error("[외부 API] page = {} 재시도 4회 모두 실패", page, e);
        throw new ExternalApiException("외부 상품 동기화 중단 (page=" + page + ")", e);
    }
}
