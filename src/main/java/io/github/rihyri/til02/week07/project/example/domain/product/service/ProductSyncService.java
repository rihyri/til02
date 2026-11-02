package io.github.rihyri.til02.week07.project.example.domain.product.service;

import io.github.rihyri.til02.week07.project.example.domain.product.entity.SyncResult;
import io.github.rihyri.til02.week07.project.example.global.external.dto.ExternalProductResponse;
import io.github.rihyri.til02.week07.project.example.global.external.dto.ExternalProductResponse.ExternalResponse;
import io.github.rihyri.til02.week07.project.example.global.external.dto.ExternalProductResponse.ExternalPage;
import io.github.rihyri.til02.week07.project.example.global.fetch.ExternalProductFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private static final int PAGE_SIZE = 50;
    private final ExternalProductFetcher fetcher;
    private final ProductUpsertService upsertService;

    public SyncResult syncAll() {
        List<ExternalResponse> collected = new ArrayList<>();
        int page = 0;
        boolean last = false;

        while (!last) {
            ExternalPage result = fetcher.fetchPage(page, PAGE_SIZE);
            List<ExternalResponse> contents = result.getContents();

            if (contents == null || contents.isEmpty()) {
                break;
            }

            collected.addAll(contents);
            last = (result.getPageable() != null) ? result.getPageable().isLast() : contents.size() < PAGE_SIZE;
            page++;
        }

        log.info("[외부 API] 총 {}페이지, {}건 수집", page, collected.size());
        return upsertService.upsert(collected);
    }
}
