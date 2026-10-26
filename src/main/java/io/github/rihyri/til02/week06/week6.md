<br>

# Week 6. Spring Boot - Transaction, Isolation & Concurrency Control

<br>

6주차에는 트랜잭션의 기본 개념과 관리 방식부터, 동시성 문제와 격리 수준, 데이터베이스 Lock을 활용한 정합성 보장 방법까지 학습했다.

5주차가 Redis와 Spring Security를 활용해 사용자 상태와 데이터 접근을 관리하는 과정이었다면, 6주차는 **여러 요청이 동시에 발생하는 환경에서 데이터를 어떻게 안전하고 일관되게 처리할 것인가**에 집중했다.

전체적인 흐름은 다음과 같다.

```aiignore
트랜잭션 관리 → 동시성 이해 문제 → 격리 수준 설정 → Lock을 통한 정합성 보장
```

<br>
<hr>

## 1. 트랜잭션 (Transaction)

트랜잭션은 여러 데이터베이스 작업을 하나의 논리적인 작업 단위로 묶어 처리하는 개념이다.

예를 들어 계좌 이체에서는 출금과 입금이 모두 성공해야 하며, 하나라도 실패하면 이전 작업까지 취소되어야 한다.

```aiignore
트랜잭션 시작
    ↓
A 계좌 잔액 감소
    ↓
B 계좌 잔액 증가
    ↓
성공 → COMMIT
실패 → ROLLBACK
```
<br>

### ACID

트랜잭션의 주요 특성은 ACID로 정리할 수 있다.

<table>
    <thead>
        <th>속성</th>
        <th>의미</th>
    </thead>
    <tbody>
        <tr>
            <td>Atomicity (원자성)</td>
            <td>모든 작업이 성공하거나 전부 취소된다.</td>
        </tr>
        <tr>
            <td>Consistency (일관성)</td>
            <td>트랜잭션 전후 데이터 무결성 규칙을 유지한다.</td>
        </tr>
        <tr>
            <td>Isolation (격리성)</td>
            <td>동시에 실행되는 트랜잭션의 간섭을 제어한다.</td>
        </tr>
        <tr>
            <td>Durability (지속성)</td>
            <td>커밋된 데이터는 장애 이후에도 유지되어야 한다.</td>
        </tr>
    </tbody>
</table>

<br>

### Spring 트랜잭션 관리 방식

Spring에서는 크게 세 가지 방식으로 트랜잭션을 관리할 수 있다.

<table>
    <thead>
        <th>방식</th>
        <th>특징</th>
    </thead>
    <tbody>
        <tr>
            <td>JDBC 직접 관리</td>
            <td>Connection을 통해 commit, rollback 직접 실행</td>
        </tr>
        <tr>
            <td>프로그래밍 방식</td>
            <td>PlatformTransactionManager로 트랜잭션 관리</td>
        </tr>
        <tr>
            <td>선언적 방식</td>
            <td>@Transactional을 이용한 자동 관리</td>
        </tr>
    </tbody>
</table>

<br>

### @Transactional

`@Transactional`은 Spring AOP를 이용하여 메서드 실행 전후로 트랜잭션을 관리한다.

```java
@Transactional
public void transfer(Long fromId, Long toId, int amount) {
    accountRepository.withdraw(fromId, amount);
    accountRepository.deposit(toId, amount);
}
```

Spring은 일반적으로 프록시를 통해 트랜잭션을 시작하고, 메서드가 정상적으로 종료되면 커밋한다.

기본적으로 `RuntimeException`과 Error가 발생하면 롤백하지만, 체크 예외에는 자동 롤백이 적용되지 않는다.

주의할 점은 다음과 같다.

- 동일한 객체 내부 호출(Self-Invocation)은 프록시를 거치지 않아 별도의 트랜잭션 설정이 적용되지 않을 수 있다.
- 트랜잭션 범위가 지나치게 길어지면 DB 연결과 Lock을 오래 점유할 수 있다.
- `readOnly = true`는 읽기 최적화 힌트이며, 모든 환경에서 쓰기 작업을 차단하는 것은 아니다.

<br>
<hr>

## 2. 동시성 문제 (Concurrency)

<br>

동시성은 여러 작업이 같은 시기에 실행되거나 진행되는 상황을 의미한다.

하나의 데이터에 여러 트랜잭션이 동시에 접근하면 예상하지 못한 결과가 나올 수 있다.

<br>

### 대표적인 동시성 문제

<table>
    <thead>
        <th>문제</th>
        <th>설명</th>
    </thead>
    <tbody>
        <tr>
            <td>Dirty Read</td>
            <td>다른 트랜잭션의 커밋되지 않은 데이터 조회</td>
        </tr>
        <tr>
            <td>Non-Repeatable Read</td>
            <td>동일한 행을 다시 조회했을 때 값이 달라짐</td>
        </tr>
        <tr>
            <td>Phantom Read</td>
            <td>동일한 조건으로 조회했을 때 결과 행의 구성이 달라짐</td>
        </tr>
        <tr>
            <td>Lost Update</td>
            <td>한 트랜잭션의 수정 결과가 다른 수정으로 덮어써짐</td>
        </tr>
    </tbody>
</table>

<br>

예를 들어 상품 재고가 10개이고 두 사용자가 동시에 7개씩 주문한다고 가정한다.

두 요청이 모두 재고를 10개로 조회하면 각각 주문 가능하다고 판단할 수 있다.

이후 두 주문이 모두 성공하면 실제 재고보다 많은 상품이 판매되는 문제가 발생한다.

이처럼 **트랜잭션이 정상적으로 커밋되었다고 해서 모든 동시성 문제가 해결되는 것은 아니다.**

<br>
<hr>

## 3. 트랜잭션 격리 수준 (Isolation Level)

<br>

격리 수준은 여러 트랜잭션이 동시에 실행될 때 서로의 변경 사항을 어느 정도까지 볼 수 있는지 결정한다.

대표적인 격리 수준은 네 가지이다.

<table>
    <thead>
        <th>격리 수준</th>
        <th>Dirty Read</th>
        <th>Non-Repeatable Read</th>
        <th>Phantom Read</th>
    </thead>
    <tbody>
        <tr>
            <td>READ UNCOMMITTED</td>
            <td>가능</td>
            <td>가능</td>
            <td>가능</td>
        </tr>
        <tr>
            <td>READ COMMITTED</td>
            <td>방지</td>
            <td>가능</td>
            <td>가능</td>
        </tr>
        <tr>
            <td>REPEATABLE READ</td>
            <td>방지</td>
            <td>방지</td>
            <td>DB 구현에 따라 다름</td>
        </tr>
        <tr>
            <td>SERIALIZABLE</td>
            <td>방지</td>
            <td>방지</td>
            <td>방지</td>
        </tr>
    </tbody>
</table>

<br>

### READ UNCOMMITTED

다른 트랜잭션에서 아직 커밋되지 않은 데이터까지 읽을 수 있다.

커밋되지 않은 값 이후 롤백될 수 있기 때문에 Dirty Read 문제가 발생한다.

<br>

### READ COMMITTED

커밋된 데이터만 조회할 수 있다.

다만 같은 트랜잭션에서 동일한 데이터를 두 번 조회할 때, 그 사이 다른 트랜잭션이 커밋했다면 서로 다른 값을 읽을 수 있다.

<br>

### REPEATABLE READ

동일 트랜잭션에서 반복 조회한 데이터의 일관성을 유지한다.

MySQL InnoDB에서는 MVCC를 이용한 일반적인 SELECT에서 동일한 스냅샷을 읽는다.

따라서 일반적인 일관된 읽기에서는 Phantom Read도 방지되지만, 잠금 조회와 UPDATE 등의 동작은 구분해야 한다.

<br>

### SERIALIZABLE

트랜잭션을 직렬 실행한 것과 동등한 결과를 보장하는 가장 엄격한 격리 수준이다.

데이터 정합성에 유리하지만, 잠금 대기나 충돌로 인해 처리 성능이 감소할 수 있다.

<br>

### MVCC (Multi-Version Concurrency Control)

MVCC는 데이터의 여러 버전을 이용하여 읽기 작업과 쓰기 작업의 충돌을 줄이는 동시성 제어 방식이다.

MySQL InnoDB는 `Undo Log` 등을 활용하여 과거 데이터 버전을 조회할 수 있도록 지원한다.

```aiignore
데이터 변경 → 이전 버전 유지 → 트랜잭션의 읽기 시점에 맞는 데이터 반환
```

격리 수준이 높다고 무조건 좋은 것은 아니며, **정합성 요구사항과 성능을 함께 고려하여 설정해야 한다.**

<br>
<hr>

## 4. 데이터베이스 Lock

<br>

Lock은 여러 트랜잭션이 동일한 데이터에 접근할 때 충돌하는 작업을 제어하는 기능이다.

격리 수준이 데이터의 가시성을 조절한다면, Lock은 특정 데이터에 대한 충돌하는 작업의 실행을 제어한다.

<br>

### Shared Lock과 Exclusive Lock

<table>
    <thead>
        <th>종류</th>
        <th>특징</th>
    </thead>
    <tbody>
        <tr>
            <td>Shared Lock (S-Lock)</td>
            <td>여러 트랜잭션의 공유 잠금 허용, 충돌하는 쓰기 제한</td>
        </tr>
        <tr>
            <td>Exclusive Lock (X-Lock)</td>
            <td>다른 트랜잭션의 충돌하는 잠금 및 쓰기 제한</td>
        </tr>
    </tbody>
</table>

<br>

MySQL에서는 다음과 같이 명시적인 Lock을 사용할 수 있다. 

```sql
-- 공유 락
SELECT * FROM products WHERE id = 1 FOR SHARE;

-- 배타적 락
SELECT * FROM products WHERE id = 1 FOR UPDATE;
```

단, InnoDB의 일반적인 SELECT는 MVCC를 사용하므로 X-Lock이 존재해도 일반적인 읽기가 항상 차단되는 것은 아니다.

<br>

### InnoDB의 주요 Lock

<table>
    <thead>
        <th>종류</th>
        <th>역할</th>
    </thead>
    <tbody>
        <tr>
            <td>Record Lock</td>
            <td>인덱스의 특정 레코드 잠금</td>
        </tr>
        <tr>
            <td>Gap Lock</td>
            <td>인덱스 레코드 사이의 간격 잠금</td>
        </tr>
        <tr>
            <td>Next-Key Lock</td>
            <td>Record Lock과 Gap Lock 결합</td>
        </tr>
        <tr>
            <td>Intention Lock</td>
            <td>테이블 내부 행에 잠금을 설정하려는 의도 표시</td>
        </tr>
    </tbody>
</table>

<br>

Lock은 검색 조건, 인덱스, 격리 수준에 따라 범위가 달라질 수 있다.

따라서 Lock을 사용할 때는 잠금 대상과 범위를 함께 고려해야 한다.

<br>
<hr>

## 5. 비관적 락과 낙관적 락

<br>

동시성 문제를 해결하는 대표적인 방법으로 비관적 락과 낙관적 락이 있다.

<br>

### 비관적 락 (Pessimistic Lock)

다른 트랜잭션과 충돌할 가능성이 있다고 가정하고, 데이터를 변경하기 전에 Lock을 획득하는 방식이다.

JPA에서는 다음과 같이 사용할 수 있다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdWithLock(Long id);
```

동작 흐름은 다음과 같다.

```aiignore
데이터 조회 및 Lock 획득 → 데이터 검증 → 수정 → COMMIT → Lock 해제
```

충돌이 빈번한 환경에서 사용할 수 있지만, Lock 대기와 Deadlock 발생 가능성을 고려해야 한다.

<br>

### 낙관적 락 (Optimistic Lock)

충돌이 자주 발생하지 않는다고 가정하고, 데이터를 수정하는 시점에 충돌 여부를 검사하는 방식이다.

JPA에서는 `@Version`을 활용할 수 있다.

```java
@Version
private Long version;
```

데이터 수정 시 기존 Version을 조건으로 사용한다.

```java
UPDATE products
SET stock = 8, version = 2
WHERE id = 1 AND version = 1;
```

다른 트랜잭션이 먼저 데이터를 수정했다면 Version이 달라지므로 충돌을 감지할 수 있다.

<br>

### 두 방식 비교

<table>
    <thead>
        <th>구분</th>
        <th>비관적 락</th>
        <th>낙관적 락</th>
    </thead>
    <tbody>
        <tr>
            <td>접근 방식</td>
            <td>충돌 사전 제어</td>
            <td>충돌 사후 감지</td>
        </tr>
        <tr>
            <td>주요 기술</td>
            <td>PESSIMISTIC_WRITE</td>
            <td>@Version</td>
        </tr>
        <tr>
            <td>적합한 상황</td>
            <td>충돌 빈도가 높은 경우</td>
            <td>충돌 빈도가 낮은 경우</td>
        </tr>
        <tr>
            <td>주의 사항</td>
            <td>Lock 대기, Deadlock</td>
            <td>충돌 예외, 재시도 처리</td>
        </tr>
    </tbody>
</table>

어느 방식이 항상 더 좋은 것은 아니며, 동시 요청 규모와 충돌 빈도에 따라 적절한 방식을 선택해야 한다.

<br>
<hr>

## 6. 동시성 테스트

<br>

동시성 문제는 단일 요청만으로 재현하기 어렵기 때문에 여러 스레드를 활용한 테스트가 필요하다.

이번 실습에서는 `ExecutorService`와 `CountDownLatch`를 활용했다.

- `ExecutorService` : 여러 작업을 실행하고 스레드 풀을 관리한다.
- `CountDownLatch` : 특정 작업이 준비되거나 완료될 때까지 다른 스레드를 대기시킨다.

```aiignore
Thread A ── 데이터 조회 ── 대기 ── 재조회
                               ↑   
Thread B ──── 데이터 변경 및 COMMIT
```

특정 실행 순서를 제어하면 Dirty Read, Non-Repeatable Read, Lost Update 등의 문제를 재현하고 해결 방법을 검증할 수 있다.

단, 테스트에서 스레드 실행 순서를 조정했다고 해서 DB 내부의 Lock 획득 순서까지 완전하게 보장되는 것은 아니다.

<br>
<hr>

### 6주차 핵심 흐름

<br>

이번 주에 학습한 기술들을 하나의 서비스에 연결하면 다음과 같다.

### 1. 트랜잭션 관리

```aiignore
요청 → 트랜잭션 시작 → DB 작업 → COMMIT / ROLLBACK
```

### 2. 동시성 문제 분석

```aiignore
동시 요청 → 동일 데이터 접근 → 격리 수준 확인 → 데이터 충돌 분석
```

### 3. 데이터 정합성 보장

```aiignore
동시성 위험 식별 → Lock 또는 조건부 UPDATE 적용 → 충돌 검증 → 결과 확정
```

이러한 과정을 통해 여러 사용자가 동일한 데이터를 수정하는 상황에서도 비즈니스 규칙을 유지할 수 있다.

<br>
<hr>

### 마치며

<br>

6주차에는 트랜잭션을 단순히 DB 작업을 묶는 기능으로 이해하는 것을 넘어, 여러 요청이 동시에 실행되는 환경에서 발생하는 문제들을 학습했다.

JDBC와 Spring의 트랜잭션 관리 방식을 비교하고, MySQL의 격리 수준과 MVCC를 통해 데이터 조회 결과가 달라지는 이유를 확인했다.

또한 비관적 락과 낙관적 락을 이용해 데이터 충돌을 제어하는 방법을 학습했다.

이번 주의 핵심 개념은 다음과 같다.

```aiignore
Transaction → 원자성과 일관성 관리
Isolation Level → 트랜잭션 간 데이터 가시성 제어
MVCC → 버전 기반 일관된 읽기
Lock → 충돌하는 작업의 실행 제어
Concurrency Test → 동시성 문제 재현 및 검증
```

6주차의 핵심은 **여러 트랜잭션이 동시에 실행되더라도 데이터의 정합성을 어떻게 보장할 것인가?** 라는 질문으로 정리할 수 있다.

<br>

