<br>

# Week 07 - Project : 외부 상품 연동 · 스케줄 동기화 · 상품 검색 구현

<br>

## 1. 구현 목표

- 외부 상품 API를 OpenFeign으로 호출해 `product` 테이블에 통합 저장
- 외부 상품 ID 기준으로 신규는 추가, 기존은 업데이트 (중복 적재 방지)
- 스케줄러로 주기적 동기화 + 실패 재시도 (최대 3회, 지연 시간 적용)
- `stock > 0` 기준으로 `is_orderable` 계산
- `GET /products`에서 카테고리·주문 가능 여부 조건 검색 + OFFSET 페이징

<br>
<hr>

## 2. 기존 코드에서 발견한 문제

<br>

<table>
    <thead>
        <th>위치</th>
        <th>문제</th>
    </thead>
    <tbody>
        <tr>
            <td>saveAllExternalProducts()</td>
            <td>@Retryable이 루프 전체에 걸려 있음 → 중간 페이지 실패시 0페이지부터 다시 호출</td>
        </tr>
        <tr>
            <td>저장 로직</td>
            <td>항상 새 Product 생성 → 동기화 할때마다 중복 누적</td>
        </tr>
        <tr>
            <td>외부 응답 처리</td>
            <td>실패해도 HTTP 200 + result=false → Feign이 예외로 인식하지 않음</td>
        </tr>
        <tr>
            <td>예외 처리</td>
            <td>모든 예외를 DomainException으로 변환 → 비즈니스 오류까지 재시도</td>
        </tr>
    </tbody>
</table>

<br>

### 현재 외부 서버는 실패를 200으로 내려준다.

```java
// 외부 서버 ApiResponse : 실패인데도 HTTP 200
return ResponseEntity.ok(new ApiResponse<>(false, code, errorMessage, null));
```

- Feign은 **HTTP 상태 코드**로 성공과 실패를 판단한다. → 200이면 정상 응답으로 처리
- `result` 값을 직접 확인해서 예외를 던져야 재시도가 동작한다.

<br>
<hr>

## 3. DB 변경

<br>

```sql
ALTER TABLE product
    ADD COLUMN external_product_id VARCHAR(50) NULL,    -- 외부 상품 ID
    ADD COLUMN source              VARCHAR(50) NOT NULL DEFAULT 'INTERNAL',  -- INTERNAL/EXTERNAL
    ADD COLUMN is_orderable        BOOLEAN     NOT NULL DEFAULT FALSE  -- 주문 가능 여부

-- 외부 상품 ID는 PK와 별도로 유니크하게 관리
ALTER TABLE product ADD CONSTRAINT uk_product_external_product_id UNIQUE (external_product_id);
```

- 기존 마이그레이션 파일은 수정하지 않고 새 파일로 추가한다.

<br>
<hr>

## 4. Product 엔티티

<br>

```java
// 외부 데이터 그대로 반영
public void syncFromExternal(String name, String description, BigDecimal price, Integer stock) {
    this.name = name;
    this.description = description;
    this.price = price;
    this.stock = stock;
    this.isOrderable = calcOrderable(stock);
}

// 주문 가능 여부 계산 규칙을 한 곳에서 관리
private static boolean calcOrderable(Integer stock) {
    return stock != null && stock > 0;
}
```

- `calcOrderable()` 메서드 하나로 내부·외부 상품 모두 같은 규칙을 적용한다.

<br>
<hr>

## 5. 외부 API 호출 클래스 구조

<br>

```aiignore
ProductSyncScheduler          매일 자정 실행 (언제 실행할지)
        │
        ▼
ProductSyncService            전체 페이지 수집 (트랜잭션 없음)
        │
        ├─▶ ExternalProductFetcher     외부 API 1페이지 호출 + 재시도
        │        └─▶ ExternalShopClient (Feign) ─▶ 외부 서버
        │
        └─▶ ProductUpsertService       DB 반영 (@Transactional)
```

- `ProductSyncScheduler` : 매일 자정 실행, 시작 시간·처리 건수·소요 시간을 로깅한다.
- `ProductSyncService` : 0페이지부터 마지막 페이지까지 반복 수집한다.
- `ExternalProductFetcher` : 외부 API 호출, `@Retryable` 재시도, `@Recover` 최종 실패 처리
- `ProductUpsertService` : 외부 ID 기존 상품 조회 후 insert / update

<br>

### 클래스를 나눠서 설계한 이유
- `@Retryable`, `@Transactional`은 **프록시** 기반 → 같은 클래스 안에서 `this.method()`로 호출하면 적용되지 않는다.
- 재시도 범위(페이지 1건)와 트랜잭션 범위(DB 반영)를 각각 별도 빈으로 분리한다.
- 스케줄러는 "언제", 서비스는 "어떻게"만 담당하도록 한다.

<br>
<hr>

## 6. 외부 API 호출과 재시도 (`ExternalProductFetcher`)

<br>

```java
@Retryable(
        retryFor = {
                RetryableException.class,                   // 연결 실패, 타임아웃
                FeignException.FeignServerException.class,  // 5xx
                ExternalApiException.class                  // HTTP 200 + result=false
        },
        maxAttempts = 4,
        backoff = @Backoff(delay = 1000, multiplier = 2)
)
public ExternalPage fetchPage(int page, int size) {
    
    ExternalProductResponse res = externalShopClient.getProducts(page, size);
    
    // 200이어도 result가 true가 아니면 실패로 판단
    if (res == null || res.getMessage() == null) {
        throw new ExternalApiException("외부 상품 조회 실패 (page =" + page + ")");
    }
    return res.getMessage();
}

// 재시도 모두 실패 시 실행 → 예외를 던져 동기화 전체 중단
@Recover
public ExternalPage recover(RuntimeException e, int page, int size) {
    throw new ExternalApiException("외부 상품 동기화 중단 (page=" + page + ")", e);
}
```

- Feign 자체 재시도는 `Retryer.NEVER_RETRY`로 꺼둔다. → 두 재시도가 겹쳐 호출 횟수가 곱해지는 것을 방지한다.
- `spring-boot-starter-aop` 의존성이 없으면 `@Retryable`이 무시되기 때문에 추가해야 한다.

<br>
<hr>

## 7. 동기화 시도

<br>

### ProductSyncService : 수집 (트랜잭션 없음)

```java
public SyncResult syncAll() {
    List<ExternalResponse> collected = new ArrayList<>();
    int page = 0;
    boolean last = false;
    
    while (!last) {
        ExternalPage result = fetcher.fetchPage(page, PAGE_SIZE);
        collected.addAll(result.getContents());
        last = result.getPageable().isLast();
        page++;
    }
    
    return upsertService.upsert(collected);
}
```

- `@Transactional`이 없음 → 외부 응답을 기다리는 동안 DB 커넥션을 잡지 않는다.

<br>

### ProductUpsertService : 저장 (하나의 트랜잭션)
```java
@Transactional
public SyncResult upsert(List<ExternalResponse> externals) {
    
    // 외부 ID로 기존 상품을 한 번에 조회한다. (IN 쿼리 1번) → Map으로 변환
    Map<String, Product> existing = productRepository.findAllByExternalProductIdIn(ids).stream()
            .collect(Collectors.toMap(Product::getExternalProductId, Function.identity()));
    
    for (ExternalResponse e : externals) {
        Product found = existing.get(String.valueOf(e.getId()));
        
        if (found == null) {
            toInsert.add(Product.fromExternal(...));    // 없으면 신규 등록
        } else if (found.syncFromExternal(...)) {
            updated++;                                  // 있으면 변경 감지로 UPDATE
        }
    }
    
    productRepository.saveAll(toInsert);
    return new SyncResult(toInsert.size(), updated);
}
```

- 기존 상품은 `save()` 없이도 커밋 시점에 JPA 변경 감지로 UPDATE된다.
- **값이 없으면 UPDATE 쿼리가 실행되지 않는다.** (조회 시점 스냅샷과 비교해 차이가 없기 때문)
- 처리 중 오류가 나면 트랜잭션 전체가 롤백된다. → 일부만 저장되는 상황을 방지

<br>
<hr>

## 8. 스케줄러

<br>

```java
// 매일 자정 실행 (주기는 yml에서 관리)
@Scheduled(cron = "${external.external-shop.sync-cron}")
public void syncExternalProducts() {
    try {
        SyncResult r = productService.syncAll();
        log.info("[상품 동기화] 완료: 신규 {}건, 수정 {}건", r.getCreated(), r.getUpdated());
    } catch (Exception e) {
        log.error("[상품 동기화] 실패, 다음 주기에 다시 시도합니다.", e);
    }
}
```
```aiignore
external:
    external-shop:
        sync-cron: "0 0 0 * * *"    # 초 분 시 일 월 요일 → 매일 00:00:00
```

- 메인 클래스에 `@EnableScheduling`을 추가한다.
- 동기화 주기는 자정 하루 1번 → 너무 잦은 호출은 외부 서버와 DB에 불필요한 부하이기 때문에 트래픽이 낮은 시간에 한 번 호출하도록 한다.
- 테스트 할 때는 `"0 * * * * *"`(매 분)으로 바꿔서 확인했다.

<br>
<hr>

## 9. 트랜잭션 설계

<br>

- 요구사항: "외부 API 호출과 등록을 하나의 트랜잭션으로 묶어 무결성 보장"
- 해결: **전체 페이지를 먼저 다 받고, DB 반영만 하나의 트랜잭션으로 관리한다.**
  - 중간 페이지가 최종 실패 → `@Recover`에서 예외 → DB에서 아무것도 반영되지 않음
  - DB 반영 중 오류 → 트랜잭션 전체를 롤백한다.

<br>

### 외부 호출을 트랜잭션 안에 두면 위험한 이유

- 응답을 기다리는 동안 DB 커넥션을 점유한다. → HikariCP 기본 풀(10개) 고갈 시 다른 API까지 대기해야 한다.
- 트랜잭션이 길어지면 락 유지 시간이 증가된다.
- 외부 호출 결과는 어차피 DB 롤백으로 되돌릴 수 없다.

<br>
<hr>

## 10. 상품 검색 `GET   /products`

<br>

### ProductQueryRepository: QueryDSL 동적 검색

```java
// 값이 있는 조건만 and로 추가 (MyBatis <if>와 같은 역할)
BooleanBuilder where = new BooleanBuilder();

if (StringUtils.hasText(categoryName)) {
    where.and(category.name.eq(categoryName));    
}

if (isOrderable != null) {
    where.and(product.isOrderable.eq(isOrderable));
}

// 목록 조회 : product + category 조인, DTO로 바로 조회
List<ProductSearchResponse> content = queryFactory
        .select(Projections.constructor(ProductSearchResponse.class,
                product.id, product.name, product.price,
                product.stock, product.isOrderable, category.name))
        .from(product)
        .join(product.category, category)
        .where(where)
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

// 전체 개수 조회 : 같은 조건으로 COUNT
Long total = queryFactory.select(product.count())
        .from(product).join(product.category, category)
        .where(where).fetchOne();

return new PageImpl<>(content, pageable, total);

```

- `Projections.constructor`는 select 순서와 DTO 생성자 파라미터 순서가 같아야 한다.
- `Custom + Impl` 구조 대신에 조회 전용 클래스 하나를 새로 작성해 구조를 단순하게 유지했다.

<br>

### ProductController

```java
@GetMapping("/products")
public ResponseEntity<ProductPageResponse> search(
        @RequestParam(required = false) String category,
        @RequestParam(name = "is_orderable", required = false) Boolean isOrderable,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    
    Pageable pageable = PageRequest.of(page - 1, pageSize);    // 사용자는 1부터, Spring은 0부터
    return ResponseEntity.ok(new ProductPageResponse(productService.search(category, isOrderable, pageable)));
}
```

### 응답 예시

```aiignore
{
  "products": [
    { "id": 105, "name": "A1B2C3D4", "price": 3000.00, "stock": 100, "is_orderable": true, "categoryName": "외부상품" }
  ],
  "total": 100,
  "page": 1,
  "page_size": 20
}
```

<br>
<hr>

## 11. 정리

- 외부 API는 실패해도 HTTP 200으로 응답할 수 있다. → 응답 본문까지 확인해야 재시도가 걸린다.
- `@Retryable`은 붙은 메서드 전체를 다시 실행한다. → 재시도 범위는 작게 잡는다.
- `@Retryable`, `@Transactional`은 같은 클래스 내부 호출에서는 동작하지 않는다.
- 외부 호출은 트랜잭션 밖에서, DB 반영만 트랜잭션 안에서 처리한다.
- 중복 적재는 외부 ID(UNIQUE) 기준 upsert로 막는다.
- 값이 바뀌지 않으면 JPA는 UPDATE를 실행하지 않는다.
- 동적 검색은 QueryDSL `BooleanBuilder`로 값이 있는 조건만 붙인다.

<br>

