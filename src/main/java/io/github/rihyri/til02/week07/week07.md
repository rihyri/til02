<br>

# Week 7. MySQL Index Optimization & Spring Transaction Propagation

<br>

7주차에는 MySQL 인덱스 설계와 실행 계획 분석을 통한 조회 성능 최적화부터, Spring 트랜잭션 전파 옵션과 롤백 전략, 외부 API 연동까지 학습했다.

6주차가 트랜잭션의 기본 개념과 동시성 제어를 통해 데이터 정합성을 보장하는 과정이었다면, 7주차는 **데이터를 효율적으로 조회하고 여러 서비스 간 작업을 안정적으로 처리하는 방법**에 집중했다.

전체적인 학습 흐름은 다음과 같다.

```azure
인덱스 설계 → 쿼리 실행 계획 분석 → 페이징 최적화
→ 트랜잭션 전파 → 트랜잭션 롤백 → 외부 API 연동
```

<br>
<hr>

## 1. MySQL 인덱스 (Index)

인덱스는 데이터베이스에서 원하는 데이터를 빠르게 찾기 위한 자료구조이다.

테이블의 모든 데이터를 확인하는 Full Table Scan 대신, 인덱스를 통해 검색 범위를 줄여 조회 성능을 향상시킬 수 있다.

<br>

### B-Tree

B-Tree는 정렬된 키를 기반으로 데이터를 탐색하는 균형 트리 자료구조이다.

- **Root Node** : 탐색을 시작하는 최상위 노드
- **Internal Node** : 키를 비교하여 탐색 경로 결정
- **Leaf Node** : 실제 검색 대상에 대한 정보 저장

MySQL InnoDB에서는 B+Tree 계열의 인덱스를 사용하며, 리프 페이지가 키 순서로 연결되어 범위 검색에 유리하다.

<br>

### 인덱스 종류

<table>
    <thead>
        <th>종류</th>
        <th>특징</th>
    </thead>
    <tbody>
        <tr>
            <td>Clustered Index</td>
            <td>기본 키를 기준으로 실제 행 데이터를 구성</td>
        </tr>
        <tr>
            <td>Secondary Index</td>
            <td>별도 검색 키와 PK 값을 저장</td>
        </tr>
        <tr>
            <td>Unique Index</td>
            <td>중복 값을 방지</td>
        </tr>
        <tr>
            <td>Composite Index</td>
            <td>여러 컬럼을 하나의 인덱스로 구성</td>
        </tr>
    </tbody>
</table>

<br>

### 복합 인덱스 (Composite Index)

두 개 이상의 컬럼을 함께 사용하는 인덱스로, 컬럼의 순서가 중요하다.

```sql
CREATE INDEX idx_category_created
ON products (category_id, created_at);
```

복합 인덱스는 기본적으로 **Leftmost Prefix (왼쪽 접두사)** 규칙을 따른다.

위 인덱스에서는 `category_id`를 기준으로 검색하거나, `category_id`와 `created_at`을 함께 사용하는 조건에서 효율적인 탐색이 가능하다.

단, 인덱스가 많아지면 저장 공간이 증가하고 INSERT, UPDATE, DELETE 성능이 저하될 수 있으므로 실제 쿼리 패턴을 고려해야 한다.

<br>
<hr>

## 2. 실행 계획과 쿼리 최적화

<br>

MySQL의 `EXPLAIN`은 SQL이 어떤 방식으로 실행될지 확인하는 명령어다.

인덱스가 존재하더라도 반드시 사용되는 것은 아니므로, 실행 계획을 분석하여 실제 접근 방식을 확인해야 한다.

<br>

### EXPLAIN 주요 항목

<table>
    <thead>
        <th>항목</th>
        <th>의미</th>
    </thead>
    <tbody>
        <tr>
            <td>type</td>
            <td>테이블 접근 방식</td>
        </tr>
        <tr>
            <td>possible_keys</td>
            <td>사용 가능한 인덱스 후보</td>
        </tr>
        <tr>
            <td>key</td>
            <td>실제 선택된 인덱스</td>
        </tr>
        <tr>
            <td>rows</td>
            <td>예상 탐색 행 수</td>
        </tr>
        <tr>
            <td>Extra</td>
            <td>정렬, 필터링 등 추가 작업 정보</td>
        </tr>
    </tbody>
</table>

대표적인 접근 방식은 다음과 같다.

- `ALL` : 전체 테이블 스캔
- `range` : 인덱스를 이용한 범위 검색
- `ref` : 인덱스를 이용한 동등 조건 검색
- `const` : 최대 한 행을 상수처럼 조회

`EXPLAIN ANALYZE`를 이용하면 예상 실행 계획뿐 아니라 실제 실행 시간과 처리 행 수도 확인할 수 있다.

<br>

### 페이징 최적화

대량 데이터를 조회할 때 페이징 방식에 따라서는 성능 차이가 발생한다.

<table>
    <thead>
        <th>방식</th>
        <th>특징</th>
    </thead>
    <tbody>
        <tr>
            <td>OFFSET/LIMIT</td>
            <td>구현이 쉽지만 뒤쪽 페이지로 갈수록 처리 비용 증가</td>
        </tr>
        <tr>
            <td>Keyset Pagination</td>
            <td>마지막 조회 위치를 기준으로 탐색하여 깊은 페이지에서도 효율적</td>
        </tr>
    </tbody>
</table>

<br>

### OFFSET 방식

```sql
SELECT *
FROM products
ORDER BY id DESC
LIMIT 10 OFFSET 50000;
```

### Keyset 방식

```sql
SELECT * FROM products
    WHERE id < 50001
ORDER BY id DESC
LIMIT 10;
```

Keyset은 앞선 데이터를 건너뛰는 대신 인덱스를 이용해 다음 조회 범위로 이동한다.

다만 실제 구현에서는 고정된 ID를 계산하는 것이 아니라 이전 페이지에서 조회한 마지막 ID를 사용해야 한다. 또한 정렬 컬럼에 중복이 있다면 PK와 같은 고유한 값을 함께 사용해 순서를 보장해야 한다.

<br>
<hr>

## 3. Spring 트랜잭션 전파 (Propagation)

<br>

트랜잭션 전파는 트랜잭션이 적용된 메서드가 다른 메서드를 호출할 때, 기존 트랜잭션에 참여할지 새로운 트랜잭션을 생성할지 결정하는 규칙이다.

<br>

### 주요 전파 옵션

<table>
    <thead>
        <th>옵션</th>
        <th>동작</th>
    </thead>
    <tbody>
        <tr>
            <td>REQUIRED</td>
            <td>기존 트랜잭션에 참여하거나 새로 생성</td>
        </tr>
        <tr>
            <td>REQUIRES_NEW</td>
            <td>기존 트랜잭션과 독립적인 새 트랜잭션 생성</td>
        </tr>
        <tr>
            <td>NESTED</td>
            <td>기존 트랜잭션 내부에 SAVEPOINT 생성</td>
        </tr>
        <tr>
            <td>SUPPORTS</td>
            <td>기존 트랜잭션이 있으면 참여하고, 없으면 트랜잭션 없이 실행</td>
        </tr>
    </tbody>
</table>

`@Transactional`의 기본 전파 옵션은 `REQUIRED`이다.

<br>

### REQUIRED와 REQUIRES_NEW의 차이

#### REQUIRED

```azure
부모 트랜잭션 시작 → 자식 메서드 실행 (같은 트랜잭션) → 예외 발생 → 전체 트랜잭션 롤백
```

부모와 자식이 동일한 물리 트랜잭션을 공유하므로, 자식 작업의 실패가 전체 롤백으로 이어질 수 있다.

#### REQUIRES_NEW

```azure
부모 트랜잭션 시작 → 부모 트랜잭션 일시 중단 → 자식 트랜잭션 시작 → 
자식 COMMIT → 부모 트랜잭션 재개 → 부모 ROLLBACK 
```

독립적으로 커밋된 자식 트랜잭션의 변경 사항은 이후 부모가 롤백되더라도 유지될 수 있다.

### NESTED와 SUPPORTS

- `NESTED`는 SAVEPOINT를 사용하여 자식 작업만 부분적으로 롤백할 수 있다. 단, 부모 트랜잭션 자체가 롤백되면 자식 작업도 함께 취소된다.
- `SUPPORTS`는 기존 트랜잭션이 있을 때만 참여하고, 트랜잭션이 없어도 실행할 수 있다.

또한 `@Async`는 별도 스레드에서 작업을 실행하므로 호출한 스레드의 트랜잭션이 자동으로 전달되지 않는다.

따라서 트랜잭션 전파 옵션뿐 아니라 **스레드와 트랜잭션 경계**도 함께 고려해야 한다.

<br>
<hr>

## 4. 트랜잭션 롤백 전략

Spring의 `@Transactional`은 메서드 실행 중 발생한 예외에 따라 커밋 또는 롤백을 결정한다.

<br>

### 예외에 따른 기본 롤백 규칙

<table>
    <thead>
        <th>예외 종류</th>
        <th>기본 동작</th>
    </thead>
    <tbody>
        <tr>
            <td>RuntimeException</td>
            <td>롤백</td>
        </tr>
        <tr>
            <td>Error</td>
            <td>롤백</td>
        </tr>
        <tr>
            <td>Checked Exception</td>
            <td>기본적으로 롤백하지 않음</td>
        </tr>
    </tbody>
</table>

체크 예외까지 롤백하려면 `rollbackFor`를 지정할 수 있다.

```java
@Transactional(rollbackFor = Exception.class)
public void saveProducts() throws Exception {
    // 상품 저장 로직
}
```

또한 `try-catch`로 예외를 처리하고 메서드가 정상 종료되면, 트랜잭션 관리자가 실패를 인식하지 못해 커밋을 시도할 수 있다.

따라서 예외를 무조건 처리하기보다는 상황에 따라 다시 던지거나 명시적으로 롤백하도록 설계해야 한다.

<br>
<hr>

## 5. 외부 API와 재시도 처리

<br>

외부 시스템에서 데이터를 가져와 저장할 때는 네트워크 오류, 응답 지연, 중복 저장 등의 문제를 고려해야 한다.

이번에는 `OpenFeign`을 사용한 상품 조회와 `Spring Retry`를 활용한 재시도 처리를 학습했다.

<br>

### OpenFeign

OpenFeign은 인터페이스 기반으로 외부 REST API를 호출할 수 있도록 지원하는 HTTP 클라이언트이다.

외부 서버의 상품 정보를 조회한 후 응답 DTO를 내부 엔티티로 변환하여 데이터베이스에 저장할 수 있다.

```azure
외부 상품 → Feign Client → 응답 DTO로 변환 
→ 상품 데이터 검증 → Product 엔티티 생성 → Database 저장 
```

<br>

### Spring Retry 

`@Retryable`을 사용하면 특정 예외가 발생했을 때 메서드를 다시 실행하도록 설정할 수 있다.

```java
@Retrayble(
    retryFor = DomaINDEXception.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000)
)
public void saveAllExternalProducts() {
    // 외부 상품 조회 및 저장
}
```

- `maxAttempts = 3` : 최초 실행을 포함하여 최대 3회 시도
- `backoff` : 재시도 사이에 대기 시간 설정
- `Retryer.NEVER_RETRY` : Feign 자체의 자동 재시도 비활성화

Feign 재시도와 Spring Retry는 서로 다른 계층에 동작한다.

특히 재시도 과정에서 동일한 상품을 다시 저장할 수 있으므로, 외부 상품 ID에 대한 유니크 제약과 Upsert 등의 중복 방지 전략이 중요하다.

<br>

### 트랜잭션과 외부 API의 관계

데이터베이스 트랜잭션은 DB 작업을 롤백할 수 있지만, 이미 외부 시스템에서 처리된 API 요청까지 되돌릴 수 없다.

이러한 문제를 해결하기 위해 `Transactional Outbox` 패턴을 활용할 수 있다.

Outbox 패턴은 비즈니스 데이터와 이벤트 발행 정보를 같은 DB 트랜잭션으로 저장한 뒤, 별도 작업에서 외부 시스템으로 이벤트를 전달하는 방식이다.

이를 통해 DB 변경과 이벤트 기록의 원자성을 확보할 수 있으며, 이벤트 재전송에 대비한 멱등성 처리도 필요하다.

<br>
<hr>

## 6. 7주차 핵심 정리

이번 주에는 데이터베이스 성능 최적화와 서비스 간 트랜잭션 처리, 외부 시스템 연동을 중심으로 학습했다.

<table>
    <thead>
        <th>학습 영역</th>
        <th>학습 내용</th>
    </thead>
    <tbody>
        <tr>
            <td>인덱스</td>
            <td>B+Tree 구조와 복합 인덱스 설계</td>
        </tr>
        <tr>
            <td>실행 계획</td>
            <td>EXPLAIN을 이용한 조회 성능 분석</td>
        </tr>
        <tr>
            <td>페이징</td>
            <td>OFFSET과 Keyset Pagination 비교</td>
        </tr>
        <tr>
            <td>트랜잭션 전파</td>
            <td>REQUIRED, REQUIRES_NEW, NESTED, SUPPORTS</td>
        </tr>
        <tr>
            <td>롤백 전략</td>
            <td>예외 종류 및 전파에 따른 커밋/롤백</td>
        </tr>
        <tr>
            <td>외부 API</td>
            <td>Feign, Retry, 트랜잭션 경계와 멱등성</td>
        </tr>
    </tbody>
</table>

<br>

인덱스와 실행 계획을 통해 SQL의 성능을 분석하는 방법을 익혔고, 트랜잭션 전파 옵션을 통해 여러 서비스가 하나의 작업을 처리할 때 트랜잭션 경계를 어떻게 설정해야 하는지 이해할 수 있다.

또한 외부 API 연동에서는 단순히 데이터를 가져와 저장하는 것뿐 아니라, **실패 상황에서 데이터 정합성을 어떻게 유지할 것인가**가 중요하다는 점을 확인했다.

<br>

