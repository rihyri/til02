<br>

# Week 07 - Day 04. 트랜잭션 롤백과 외부 API 연동 

<br>

## 1. 트랜잭션 롤백 전략

<br>

트랜잭션은 여러 DB 작업을 하나의 작업 단위로 묶어, 정상 처리 시 커밋하고 실패 시 롤백하는 기능이다. Spring의 `@Transactional`은 **기본 설정에서** `RuntimeException` 및 `Error`에 대해 롤백하며, 일반적인 체크 예외(`Exception`의 하위 타입 중 `RuntimeException`이 아닌 예외)에 대해서는 자동 롤백하지 않는다.

<br>

<table>
    <thead>
        <th>예외</th>
        <th>예시</th>
        <th>기본 롤백</th>
    </thead>
    <tbody>
        <tr>
            <td>언체크 예외</td>
            <td>IllegalArgumentException, NullPointerException</td>
            <td>O</td>
        </tr>
        <tr>
            <td>체크 예외</td>
            <td>IOException, SQLException</td>
            <td>X</td>
        </tr>
    </tbody>
</table>

이 표는 Spring의 일반적인 기본 설정 기준이다. 사용자 정의 설정에 따라 동작을 변경할 수 있다.

<br>

```java
// 원래는 자동 롤백하지 않는 체크 예외를 롤백 대상으로 지정
@Transactional(rollbackFor = IOException.class)
public void importData() throws IOException { /* ... */ }

// noRollback
@Transactional(noRollbackFor = IllegalArgumentException.class)
public void updateData() { /* ... */ }
```

<br>

### 예외를 잡아만 두면 왜 롤백되지 않을까?

```java
@Transactional
public void save() {
    try {
        // DB에서 테이블 저장
        throw new IllegalArgumentException("저장 오류");
    } catch (RuntimeException e) {
        // 로그만 남기고 정상 종료하면, 트랜잭션 경계에는 예외가 전달되지 않음
        log.error("오류 발생", e);
    }
}
```

예외를 내부에서 처리하고 정상 종료하면 트랜잭션 인터셉터는 일반적으로 커밋을 시도한다. 

롤백하려면 예외를 다시 던지거나, 필요할 때는 명시적으로 롤백 전용 상태를 설정한다. 실제로 이미 롤백 전용으로 표시된 트랜잭션 등에서는 별도의 결과가 발생할 수 있다.

<br>
<hr>

## 2. 외부 API와 DB 트랜잭션

<br>

상품 저장 로직에서 외부 서버를 호출하면 두 시스템이 관여한다.

```aiignore
메인 프로젝트 (MySQL)
  └─ ProductExternalService
       ├─ ExternalShopClient → HTTP 요청 → 외부 API 프로젝트 (H2)
       ├─ 응답 DTO 변환
       └─ ProductRepository.saveAll() → MySQL에 저장
```

**MySQL 트랜잭션은 HTTP로 호출한 외부 서버의 H2 작업을 함께 롤백하지 못한다.** 

외부 결제 등 다른 시스템에 쓰기 요청을 하는 경우에는 보상 작업, 멱등성 키, Outbox 같은 별도 설계가 필요하다.

<br>

### Transactional Outbox 개념

예시에서는 `OrderService.placeOrder()`가 주문 저장과 Outbox 이벤트 저장을 **같은 로컬 트랜잭션**으로 처리한다. `OutboxEventPublisher`가 나중에 이벤트를 조회하여 외부 결제 API를 호출한다. 같은 트랜잭션에 저장하는 것은 주문과 이벤트 기록이지, 결제 API 호출 자체가 아니다.

- `idempotencyKey` : 재전송 시 중복 처리를 방지하기 위한 식별자. **수신 시스템도 키를 검사해야** 효과가 있다.
- `READY` : 발송 대기 상태
- `SENT` : 요청 완료로 표시한 상태
- `FAILED` : 실패 상태

<br>
<hr>

## 3. Spring Retry와 Feign 재시도의 차이

<br>

```java
@Retryable(value = DomainException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 1000))
public void saveAllExternalProducts() { /* ... */}
```

- `maxAttempts = 3` : 최초 1회와 재시도 2회를 합쳐 최대 3회 실행한다.
- `delay = 1000` : 재시도 사이의 초기 지연 시간으로 1초를 사용한다.
- 예외가 메서드 밖으로 전파되고 Spring Retry 프록시를 통과할 때 적용된다.

```java
@Bean
public Retryer feignRetryer() {
    return Retryer.NEVER_RETRY;
}
```

이 설정은 **Feign 자체의 재요청을 사용하지 않겠다**는 뜻이다.

한편 `@Retryable`은 서비스 메서드 자체를 다시 수행할 수 있다. 따라서 두 설정은 모순이 아니다. 단, 실패 시 `saveAllExternalProducts()` 전체가 다시 실행되므로 앞선 페이지에 대한 요청과 저장도 반복될 수 있다. 같은 데이터를 중복 저장하지 않도록 고유 키나 업서트 전략 등을 검토해야 한다.

**추가 주의:** `@Transactional`과 `@Retryable`의 실제 프록시 적용 순서는 설정과 버전에 따라 달라질 수 있다. 반복 시 새 트랜잭션이 열리는지 로그나 테스트로 확인하는 편이 안전하다. 내부 메서드를 `this.method()`로 호출하면 프록시 기반 Retry가 적용되지 않을 수도 있다.

<br>
<hr>

## 4. Feign 타임아웃

<br>

```java
@Bean
public Request.Options feignOptions() {
    return new Request.Options(
            10000, TimeUnit.MILLISECONDS,   // connectTimeout: 연결 단계 최대 10초 
            60000, TimeUnit.MILLISECONDS,   // readTimeout: 응답 읽기 최대 60초
            true    // 리다이렉트 허용
    );
}
```

연결 제한 시간은 연결을 수립하는 과정에, 읽기 제한 시간은 연결 후 응답을 기다리는 과정에 적용된다. **최대 요청 전체 시간이 70초로 정확히 제한된다는 뜻은 아니다.** 단일 긴 db 트랜잭션 안에서 네트워크 요청을 기다리면 자원이 오래 묶일 수 있다.

<br>
<hr>

## 5. 첨부 외부 API 서버 분석 

<br>


<table>
    <thead>
        <th>원본 파일</th>
        <th>역할</th>
    </thead>
    <tbody>
        <tr>
            <td>ExternalShopApplication.java</td>
            <td>외부 서버 시작</td>
        </tr>
        <tr>
            <td>ProductController.java</td>
            <td>/products GET·POST 엔드포인트 제공</td>
        </tr>
        <tr>
            <td>ProductService.java</td>
            <td>테스트 상품 생성, 페이지 조회</td>
        </tr>
        <tr>
            <td>ProductRepository.java</td>
            <td>외부 서버 H2 DB 조회·저장</td>
        </tr>
        <tr>
            <td>Product.java</td>
            <td>외부 서버의 상품 엔티티</td>
        </tr>
        <tr>
            <td>ApiResponse.java</td>
            <td>result / error / message 응답 구조</td>
        </tr>
    </tbody>
</table>

<br>

### 실제 외부 서버의 두 요청

```aiignore
POST    http://localhost:3001/products
```
외부 서버는 먼저 기존 상품을 지우고 랜덤한 이름을 가진 상품 100개를 새로 저장한다. 실습 데이터 초기화 목적이며, 매번 상품명이 달라진다.

```aiignore
GET     http://localhost:3001/products?page=0&size=10
```

<br>

### 실제 JSON 응답의 형태

```aiignore
{
  "result": true,
  "error": {"errorCode": null, "errorMessage": null},
  "message": {
    "contents": [{"id": 1, "name": "...", "description": "...", "price": 3000, "stock": 100}],
    "pageable": {"pageNumber": 1, "pageSize": 10, "last": false}
  }
}
```

위 데이터는 **필드 구조를 보여 주는 축약 예시**이며, 실제 ID와 이름은 실행할 때 달라진다.

`ApiResponse.CustomPage.ApiPageable`은 응답에 표시할 페이지 번호에 1을 더하지만, 요청의 `page` 파라미터는 Spring `Pageable` 기준 **0부터 시작**한다.

<br>
<hr>

## 6. 메인 프로젝트의 상품 가져오기 코드 복기

<br>

### 6.1 `OpenFeignConfig`

`connectTimeout=10초`, `readTimeout=60초`, `Retryer.NEVER_RETRY`를 설정한다. Feign 단에서 자동 재요청 하지 않고 서비스 계층의 `@Retryable`을 관찰하는 구성이다.

<br>

### 6.2 `ExternalShopClient`

```java
@GetMapping("/products")
ExternalProductResponse getProducts(
        @RequestParam("page") Integer page,
        @RequestParam("size") Integer size
);
```

주소는 `external.external-shop.url` 프로퍼티에서 읽는다.

<br>

### 6.3 `ExternalProductResponse`

`result`는 성공 여부, `error`는 실패 정보, `message`는 `contents`와 `pageable`을 보유한다.

`ExternalResponse`에 들어 있는 상품 이름·설명·가격·재고를 메인 프로젝트의 `Product`로 옮긴다.

<br>

### 6.4 `ProductExternalService.save()`

1. `getProducts(1, 10)`으로 외부 서버의 **요청 인덱스 1**페이지를 조회한다. **첫 페이지(인덱스 0)가 아니라 두 번째 페이지**이다.
2. 응답의 상품 목록이 비어 있으면 `DomainException`을 발생시킨다.
3. MySQL에서 `categoryRepository.findById(1L)`로 카테고리를 조회한다.
4. 외부 상품별로 메인 프로젝트 `Product.builder()`를 사용해 엔티티를 생성한다.
5. `productRepository.saveAll(products)`로 저장한다.
6. 원본 `catch (Exception)`은 원래 예외 종류를 가지지 않고 `NOT_FOUND_PRODUCT`로 변경한다. 이로 인해 네트워크 장애도 '상품 없음'으로 보일 수 있다.

<br>

### 6.5 `ProductExternalService.saveAllExternalProducts()`

1. `page = 0`, `pageSize = 10`, `lastPage = false`로 시작한다.
2. 외부 상품 목록을 페이지별로 요청한다.
3. 응답이나 상품이 없다면 예외를 던지거나 루프를 종료한다.
4. 카테고리 ID 1을 찾아 모든 상품에 연결한다.
5. 해당 페이지의 상품을 `saveAll()`로 저장한다.
6. `pageable.isLast()` 또는 응답 개수로 마지막 페이지를 판별한다.
7. 마지막 페이직가 아니라면 `page++` 후 반복한다.

**트랜잭션 범위 :** 메서드에 `@Transactional`이 있으므로 일반적인 설정에서는 **페이지별로 커밋하는 것이 아니라 전체 메서드가 하나의 트랜잭션**이다. 중간 페이지에서 예외가 최종 전파되면 앞서 저장했던 페이지의 작업도 롤백 대상이 된다.

<br>

## 7. 정리

- 체크 예외와 언체크 예외의 **기본 롤백 규칙**은 다르다.
- 예외를 삼키면 트랜잭션 프록시가 실패를 모를 수 있다.
- DB 트랜잭션은 외부 API의 부작용까지 원자적으로 묶지 못한다.
- Feign의 `Retryer`와 서비스의 `@Retryable`은 다른 계층의 설정이다.

<br>



