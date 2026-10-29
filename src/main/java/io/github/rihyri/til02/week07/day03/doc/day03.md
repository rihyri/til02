<br>

# Week 07 - Day 03. 트랜잭션 전파 옵션

<br>

## 1. 학습 목표

<br>

- `@Transactional`의 `전파(Propagation)`가 무엇을 결정하는지 이해한다.
- `REQUIRED`, `REQUIRES_NEW`, `NESTED`, `SUPPORTS`를 **직접 실행하여 비교**한다.
- 주문 접수(`orderRequest`), 주문 처리(`orderProcess`), 작업 큐(`processQueueById`)가 각각 어느 트랜잭션에 속하는지 구분한다.
-  `@Async`와 예외 처리에 따라 커밋/롤백 결과가 달라지는 이유를 설명한다.
- 이번 예제에서는 JPA 대신 Spring JDBC(JdbcTemplate)를 사용해 `NESTED`의 SAVEPOINT 동작을 직접 확인한다.

<br>
<hr>

## 2. 트랜잭션 전파란?

<br>

트랜잭션은 여러 DB 작업을 하나로 묶는 작업 단위이다. **커밋(COMMIT)** 은 변경을 확정하고, **롤백(ROLLBACK)** 은 해당 트랜잭션의 변경을 취소한다. **전파 옵션**은 트랜잭션 메서드가 다른 트랜잭션 메서드를 호출할 때 *기존 트랜잭션에 합류할지, 새로 시작할지, 트랜잭션 없이 수행할지*를 결정한다.

```aiignore
OrderService.orderRequest() ──호출──▶ OrderProcessService.orderProcess();
            [부모]                                  [자식]
```

<table>
    <thead>
        <th>전파 옵션</th>
        <th>부모 트랜잭션이 있으면</th>
        <th>없으면</th>
        <th>핵심</th>
    </thead>
    <tbody>
        <tr>
            <td>REQUIRED</td>
            <td>같은 물리 트랜잭션에 참여</td>
            <td>새로 시작</td>
            <td>전체 작업을 한 묶음으로</td>
        </tr>
        <tr>
            <td>REQUIRES_NEW</td>
            <td>부모를 잠시 보류하고 독립 트랜잭션 시작</td>
            <td>새로 시작</td>
            <td>자식만 독립 커밋 가능</td>
        </tr>
        <tr>
            <td>NESTED</td>
            <td>부모 트랜잭션 내 SAVEPOINT 생성</td>
            <td>새로 시작</td>
            <td>자식 작업만 부분 롤백 가능</td>
        </tr>
        <tr>
            <td>SUPPORTS</td>
            <td>기존 트랜잭션 참여</td>
            <td>트랜잭션 없이 수행</td>
            <td>간단한 조회에 활용</td>
        </tr>
    </tbody>
</table>

`@Transactional`의 기본 전파 값은 `REQUIRED`이다. 기본적으로 **`RuntimeException`** 과 **`Error`** 에 롤백하며, 체크 예외는 별도 `rollbackFor` 설정이 없으면 자동 롤백하지 않는다.

<br>
<hr>

## 3. REQUIRED - 함께 성공하거나 함께 실패

<br>

```java
@Transactional(propagation = Propagation.REQUIRED)
public void required(OrderRequest request) {
    long queueId = taskQueueService.requestQueue();
    orderProcessService.orderProcessRequired(queueId, request, true);
}
```

- `required()`에서 트랜잭션 A 시작
- `requestQueue()`는 A에 참여하여 큐를 INSERT
- `orderProcessRequired()`도 A에 참여하여 주문과 상품을 INSERT
- 마지막에 의도적 예외가 발생하여 **트랜잭션 A 전체가 롤백**

```aiignore
A 시작 ─ 큐 INSERT ─ 주문 INSERT ─ 상품 INSERT ─ 예외 ─ A ROLLBACK

결과: 큐 X, 주문 X, 상품 X
```


<br>
<hr>

## 4. REQUIRES_NEW - 독립적으로 기록 남기기

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveLog(String message) {
    jdbc.update("INSERT INTO audit_log(message) VALUES (?)", message);
}
```

```aiignore
부모 A 시작 ─ 큐 INSERT ─ [A 잠시 보류]
                             └─ 자식 B 시작 ─ 로그 INSERT ─ B COMMIT
              [A 재개] ─ 의도적 예외 ─ A ROLLBACK
```

- **서로 다른 물리 트랜잭션**을 사용한다.
- 자식 B가 커밋되었다면 나중에 부모 A가 롤백하더라도 이미 저장된 B의 로그는 유지된다.
- 단, **호출 전에 부모가 실패하여 자식이 호출조차 되지 않았더라면** 자식의 작업도 실행되지 않는다.
- JDBC 커넥션이 추가로 필요하므로 커넥션 풀 크기도 고려해야 한다.

<br>
<hr>

## 5. NESTED - SAVEPOINT로 부분 롤백

<br>

```java
@Transactional(propagation = Propagation.NESTED)
public void orderProcessNested(long queueId, OrderRequest request, boolean fail) {
    process(queueId, request, fail);
}
```
```aiignore
A 시작 ─ 큐 INSERT ─ SAVE POINT S ─ 주문/상품 INSERT ─ 자식 예외
                     ◀──── S까지 ROLLBACK ────┘
             부모에서 예외 처리 ─ A COMMIT
결과: 큐 O / 주문 X / 상품 X
```

`NESTED`는 새 물리 트랜잭션이 아니다. 하나의 DB 트랜잭션에 중간 복구 지점(SAVEPOINT)을 만드는 방식이다. 따라서 **자식만 실패할 때 부모 작업을 유지할 수 있지만, 부모가 최종 롤백하면 자식 작업도 모두 사라진다.**

> **환경 조건**: SAVEPOINT를 지원하는 MySQL InnoDB와 JDBC 트랜잭션 매니저를 사용한다. JPA의 `JpaTransactionManager`에서는 동일하게 작동한다고 가정하면 안 된다.

<br>
<hr>

## 6. SUPPORTS - 있으면 참여, 없으면 그대로 조회

```java
@Transactional(propagation = Propagation.SUPPORTS)
public String getUser(long userId) {
    String name = jdbc.queryForObject(
            "SELECT name FROM users WHERE id = ?", String.class, userId);
    return "user=" + name + ", txActive=" +
            TransactionSynchronizationManager.isActualTransactionActive();
}
```

`/demo/supports/outside/1`은 트랜잭션이 없는 컨트롤러에서 직접 호출하므로 `txActive=false`가 표시된다. 

`/demo/supports/inside/1`은 `REQUIRED` 메서드 내부에서 호출하므로 `txActive=true` 이다. 

`SUPPORTS`가 **항상 읽기 전용을 강제하는 것은 아니다.** 단순 조회에 활용하기 편리한 옵션일 뿐이다.

<br>
<hr>

## 7. @Async와 트랜잭션 전파

<br>

`orderRequest()` → `@Async orderProcess()` → `processQueueById()` 흐름을 별도로 구현했다.

```aiignore
요청 스레드: orderRequestAsync() ─ 큐 INSERT ─ Tx-A COMMIT
                                              │
                            커밋 완료 후 비동기 호출
                                              ▼
별도 스레드:          @Async orderProcess() ─ Tx-B 시작
                                      processQueueById(REQUIRED)
                                      주문/상품/총액 저장 ─ Tx-B COMMIT
```

- `@Async` 메서드는 별도 스레드에서 실행된다. 호출자의 스레드에 연결된 Tx-A가 자동으로 이어지지 않는다.
- 새 스레드에 기존 트랜잭션이 없다면 `REQUIRED`와 `REQUIRES_NEW` 모두 새 트랜잭션을 시작한다. **두 옵션의 차이를 비교하려면 동기 호출이 더 적합하다.**
- `@Async`와 `@Transactional`은 스프링 프록시를 통해 호출되어야 한다. 같은 객체의 `this.method()`로 호출하면 의도대로 적용되지 않을 수 있다.

<br>
<hr>

## 8. 자주 헷갈리는 부분

<br>

**· REQUIRES_NEW는 부모가 실패하면 무조건 자식도 실패하나요?**

→ 아니다.자식이 호출되고 정상 커밋했다면 독립적으로 남는다.

**· nested는 자식만 독립 커밋할 수 있나요?**

→ 아니다. SAVEPOINT에서 부분 롤백할 수 있지만 최종 커밋은 부모 트랜잭션의 커밋에 달려 있다.

**· 부모가 자식 예외를 `catch`하면 무조건 커밋되나요?**

→ 아니다. 이미 rollback-only로 표시된 트랜잭션은 커밋할 수 없다. 특히 `REQUIRED` 내부의 예외 전파를 주의해야 한다.

**· `@Async`와 `REQUIRES_NEW`를 같이 써야 분리되나요?**

→ `@Async`로 다른 스레드에 진입하면 호출자의 트랜잭션이 전파되지 않는다. 새 스레드에서 `REQUIRED`도 새 트랜잭션을 시작할 수 있다.

**· `SUPPORTS`는 쓰기에 절대 사용할 수 없나요?**

→ 기술적으로 금지되지는 않지만, 트랜잭션이 없는 상태에서 실행될 수 있어 원자성이 필요한 데이터 변경에는 부분적으로 부적합하다.

<br>
<hr>

## 9. 개념을 더 깊이 이해하기

<br>

### 9.1 트랜잭션과 전파 옵션을 구분하기

트랜잭션 자체는 **DB 변경을 하나의 단위로 처리하는 기능**이고, 전파 옵션은 **그 단위 안에서 다른 서비스 메서드가 실행될 때 어느 트랜잭션을 사용할지 결정하는 규칙**이다.

예를 들어 주문 기능은 다음 단계로 나눌 수 있다.

1. `orderRequest()`에서 요청을 받는다.
2. `requestQueue()`에서 작업 큐를 저장한다.
3. `orderProcess()`에서 주문 데이터를 생성한다.
4. `processQueueById()`에서 큐 상태를 변경하고 주문 상품을 저장한다.
5. 주문 총액을 계산한 뒤 처리를 끝낸다.

DB 작업에서는 중간에 실패했을 때 **앞에서 실행한 INSERT와 UPDATE를 확정할지, 취소할지**가 중요하다. `@Transactional`은 이 범위를 정한다.

<br>

### 9.2 논리적 트랜잭션과 물리적 트랜잭션

Spring 공식 문서는 `REQUIRED`를 설명할 때 **논리적 트랜잭션 범위(logical transaction scope)** 와 **물리적 트랜잭션(physical transaction)** 을 구분한다.

- **논리적 범위 :** `@Transactional`이 적용된 각 메서드 관점의 트랜잭션 경계이다.
- **물리적 트랜잭션 :** 실제 데이터베이스 커넥션을 통해 커밋·롤백되는 DB 작업 단위이다.

```aiignore
OrderService.required()                   ┐ 논리적 범위 1
  └─ TaskQueueService.requestQueue()      │ 논리적 범위 2
  └─ OrderProcessService.orderProcessRequired() │ 논리적 범위 3
       └─ TaskQueueService.processQueueById()  │ 논리적 범위 4
                                              ┘
                  모두 하나의 물리 트랜잭션 A를 공유 (REQUIRED)
```

`REQUIRED` 메서드가 네 번 등장한다고 DB 트랜잭션이 네 번 생성되는 것은 아니다. 처음에 시작한 하나의 물리 트랜잭션에 참여하는 형태이다. 

반대로 `REQUIRES_NEW`는 실제로 **독립된 물리 트랜잭션**을 시작한다. 

`NESTED`는 같은 물리 트랜잭션 안에 **SAVEPOINT**를 만든다.

<br>

### 9.3 `@Transactional`은 언제 시작하고 종료될까?

Spring이 관리하는 다른 Bean의 `@Transactional` 메서드를 **프록시를 통해 호출**하면, Spring은 메서드 실행 전 트랜잭션을 준비하고 실행 결과에 따라 완료한다.

```aiignore
호출자 → Spring 트랜잭션 프록시 → 트랜잭션 시작/참여
                                ↓
                        실제 서비스 메서드 실행
                                ↓
                       정상 반환 / 예외 발생
                                ↓
                         COMMIT / ROLLBACK
```

따라서 `@Transactional`을 선언한 메서드 내부에서 INSERT를 실행하는 순간 즉시 커밋된다고 생각하면 안 된다. 기존 트랜잭션에 참여 중이라면 **최종 커밋 시점은 바깥쪽 트랜잭션이 종료될 때**이다.

<br>
<hr>

## 10. 전파 옵션별 코드 따라 읽기

<br>

### 10.1 REQUIRED : 세 메서드가 함께 롤백되는 이유

실제 `OrderService.java`의 `required()`는 아래처럼 실행된다.

```java
@Transactional(propagation = Propagation.REQUIRED)
public void required(OrderRequest request) {
    long queueId = taskQueueService.requestQueue();
    orderProcessService.orderProcessRequired(queueId, request, true);
}
```

여기서 중요한 것은 **두 서비스가 모두 같은 트랜잭션 A에 참여**한다는 점이다.

| 실행 순서 | 메서드 | DB에서 일어나는 작업 | 트랜잭션 |
|---|---|---|---|
| 1 | `required()` | 트랜잭션 시작 | A |
| 2 | `requestQueue()` | `task_queue`에 `WAITING` INSERT | A |
| 3 | `orderProcessRequired()` | 주문 처리 메서드 호출 | A |
| 4 | `processQueueById()` | 큐 잠금, `PROCESSING` 변경 | A |
| 5 | 주문 처리 람다 | 주문/상품 저장, `event_id`, 총액 변경 | A |
| 6 | `processQueueById()` | `COMPLETED` 변경 | A |
| 7 | `orderProcessRequired()` | 의도적 `IllegalStateException` 발생 | A |
| 8 | Spring | 변경 전체 롤백 | A |

7단계에서 예외가 발생하기 전까지 큐 상태를 `COMPLETED`로 변경했더라도 **A가 커밋되지 않았기 때문에** 모든 변경이 취소된다.

<br>

### 10.2 REQUIRES_NEW : 왜 로그만 남을까?

`OrderService.requiresNew()`의 핵심은 다음과 같다.

```java
@Transactional(propagation = Propagation.REQUIRED)
public void requiresNew(OrderRequest request) {
    taskQueueService.requestQueue();
    auditService.saveLog("주문 요청을 받았습니다.");
    throw new IllegalStateException("부모 트랜잭션의 의도적인 오류");
}
```

`AuditService.saveLog()`는 `REQUIRES_NEW`이므로 A를 일시 보류하고 B를 시작한다.

```aiignore
부모 A : 시작 ── 큐 저장 ── 일시 보류 ────────── 재개 ── 예외 ── ROLLBACK
부모 B :                시작 ─── 로그 저장 ─── COMMIT
```

B가 먼저 확정한 `audit_log`는 A의 롤백 대상이 아니다. **그렇다고 자식의 성공이 무조건 보장되는 것은 아니다.** B 내부에서 예외가 나면 B도 롤백될 수 있고, B를 호출하기 전에 A에서 예외가 발생하면 B는 시작조차 하지 않는다.

또한 부모가 아직 수정 중인 **미커밋 데이터를 자식 B가 마음대로 읽을 수 있다는 뜻도 아니다.** 부모·자식이 별도 커넥션을 쓰므로 잠금과 격리 수준의 영향을 받을 수 있다. 이런 이유로 기존 주문을 자식 트랜잭션에서 즉시 읽거나 수정하는 예제보다 **서로 독립적인 감사 로그 저장**이 실습에 적합하다.

<br>

### 10.3 NESTED : SAVEPOINT는 '중간 저장'이 아니다.

수업 자료에서 가장 혼동하기 쉬운 부분이다. SAVEPOINT는 데이터를 최종 확정하는 COMMIT이 아니라 **되돌아갈 위치를 표시하는 지점**이다.

```sql
-- 개념을 보여주는 MySQL 예시
START TRANSACTION;
INSERT INTO task_queue(status) VALUES ('WAITING');
SAVEPOINT order_start;
INSERT INTO task_queue(status) VALUES ('PROCESSING');
ROLLBACK TO SAVEPOINT order_start;
COMMIT;
```

마지막에는 `WAITING` 행만 남는다. 실제 Java 실습에서는 두 번째 INSERT 대신 주문 생성 및 큐 변경 작업이 SAVEPOINT 이후에 일어난다.

`OrderService.nested()` 의 코드를 살펴보면 다음과 같다.

```java
@Transactional(propagation = Propagation.REQUIRED)
public void nested(OrderRequest request) {
    long queueId = taskQueueService.requestQueue();
    try {
        orderProcessService.orderProcessNested(queueId, request, true);
    } catch (IllegalStateException e) {
        System.out.println("자식 오류 처리: " + e.getMessage());
    }
}
```

**실패 시나리오:**
1. 부모 A가 큐를 저장한다. 상태는 `WAITING` 이다.
2. 자식 `orderProcessNested()`에 진입하며 SAVEPOINT가 생성된다.
3. 자식이 큐 상태를 바꾸고 주문 및 상품을 저장한다.
4. 자식 메서드에서 예외가 발생하여 SAVEPOINT까지 롤백한다.
5. 부모의 `catch`가 예외를 처리한다.
6. 부모 A가 정상 종료되어 최종 COMMIT 한다.

따라서 `task_queue`에는 `WAITING` 상태의 행이 남지만, 주문·상품 저장은 사라진다. **자식이 성공한 후라도 부모 A가 마지막에 롤백되면 모든 데이터가 취소된다.** 이 점이 독립 커밋되는 `REQUIRES_NEW`와 가장 큰 차이다.

> 이 실습의 `NESTED`는 Spring JDBC 트랜잭션 매니저와 InnoDB SAVEPOINT 지원을 전제로 한다. JPA 환경에서 똑같이 작동하는 보편적인 기능으로 생각하며 안 된다.

<br>

### 10.4 SUPPORTS: 조회 결과는 같아도 실행 환경은 다르다.

```java
@Transactional(propagation = Propagation.SUPPORTS)
public String getUser(long userId) {
    String name = jdbc.queryForObject(
            "SELECT name FROM users WHERE id = ?", String.class, userId);
    return "user=" + name + ", txActive=" +
            TransactionSynchronizationManager.isActualTransactionActive();
}
```

- `GET /demo/supports/outside/1` : 트랜잭션이 없는 컨트롤러에서 직접 호출하므로 `txActive=false`이다.
- `GET /demo/supports/inside/1` : `OrderService.supportsInside()`의 `REQUIRED` 트랜잭션에 참여하므로 `txActive=true`이다.

두 호출 모두 사용자를 조회하지만, **기존 트랜잭션에 참여하는지**가 다르다. `SUPPORTS`는 조회용으로 활용하기 좋지만 쓰기 작업을 자동으로 막아주지는 않는다. 변경 여러 건이 반드시 함께 성공해야 할 때는 `REQUIRED`처럼 명확한 경계를 정해야 한다.


<br>
<hr>

## 11. 실습에서 중요한 보충 개념

<br>

### 11.1 예외를 catch했다고 무조건 커밋되는 것은 아니다.

다음처럼 `REQUIRED` 자식 메서드가 일부러 예외를 던져 Spring이 그 트랜잭션 범위를 롤백 처리하면, **같은 물리 트랜잭션 A가 rollback-only로 표시**될 수 있다. 부모가 예외를 잡고 정상 반환하더라도 최종 커밋 시 `UnexpectedRollbackException`이 발생할 수 있다.

```java
@Transactional
public void parent() {
    try {
        childService.requiredChild(); // REQUIRED, 실행 중 런타임 예외 발생
    } catch (IllegalStateException e) {
        System.out.println("예외는 잡았습니다.");
    }
    // 트랜잭션이 이미 rollback-only라면 정상 커밋되지 않는다.
}
```

반면 이번 `NESTED` 실습에서는 자식 범위가 **SAVEPOINT로 롤백**되므로 부모가 예외를 처리하고 계속 진행하는 결과를 관찰하도록 만들었다. 단, 부모가 이런 이유로 rollback-only가 된 상태라면 최종 커밋할 수 없다.

<br>

### 11.2 기본 롤백 대상과 `rollbackFor`

Spring의 일반적인 기본 규칙은 다음과 같다.

<table>
    <thead>
        <th>예외 유형</th>
        <th>기본 롤백 여부</th>
    </thead>
    <tbody>
        <tr>
            <td>RuntimeException (예: IllegalStateException)</td>
            <td>롤백</td>
        </tr>
        <tr>
            <td>Error</td>
            <td>롤백</td>
        </tr>
        <tr>
            <td>체크 예외 (예: IOException)</td>
            <td>기본적으로 자동 롤백되지 않음</td>
        </tr>
    </tbody>
</table>

```java
@Transactional(rollbackFor = IOException.class)
public void saveWithCheckedException() throws IOException {
    // DB 작업 후 IOException이 발생해도 롤백하도록 명시
}
```

<br>
<hr>

## 12. 정리

**Q1.** 부모와 자식이 모두 `REQUIRED`일 때 자식이 런타임 예외로 실패하면 어떤 데이터가 롤백될까요?

**A1.** 하나의 물리 트랜잭션을 공유하므로, 예외가 전파되어 롤백되면 그 트랜잭션 내 부모·자식 변경이 모두 취소된다.

**Q2.** `REQUIRES_NEW`에서 부모 A가 롤백된 후에도 자식 B의 로그가 남는 이유는 무엇일까요?

**A2.** B는 A와 다른 물리 트랜잭션이며 이미 독립적으로 커밋했기 때문이다.

**Q3.** `NESTED`에서 자식 작업만 되돌리고 부모가 커밋할 수 있는 이유는 무엇일까요?

**A3.** 자식 진입 시 SAVEPOINT를 생성하여 자식 범위의 변경만 해당 지점으로 롤백할 수 있기 때문이다.

**Q4.** `@Async` 메서드가 `REQUIRED`가 붙어 있으면 호출자의 트랜잭션에 참여할까?

**A4.** 일반적으로 참여하지 않는다. 별도 스레드에 호출자의 스레드 바인딩된 트랜잭션이 자동 전파되지 않으므로, 해당 스레드에 기존 트랜잭션이 없다면 새로 시작한다.

**Q5.** `SUPPORTS`로 단순 조회할 때 트랜잭션이 없어도 메서드가 실행될까?

**A5.** 기존 트랜잭션이 없으면 새 트랜잭션을 강제로 만들지 않고 실행한다.

<br>

→ **`REQUIRED`는 함께, `REQUIRES_NEW`는 별도로, `NESTED`는 부분 되돌리기, `SUPPORTS`는 있으면 참여이다.**

<br>