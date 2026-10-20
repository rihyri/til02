<br>

# Week 06 - Day 01. - 트랜잭션 이해와 관리 방식

<br>

## 1. 트랜잭션이 필요한 이유

<br>

트랜잭션은 여러 DB 작업을 **하나의 논리적 작업 단위**로 처리한다. 예를 들어 A가 B에게 2,000원을 이체하면 `(1) A의 잔액 감소`와 `(2) B의 잔액 증가`가 모두 성공해야 한다. 도중 오류가 발생하면 이를 실행된 변경도 되돌려야 데이터가 일관된다.

### ACID 핵심

- `Atomicity(원자성)` : 관련 작업은 모두 반영되거나 모두 취소된다.
- `Consistency(일관성)` : 트랜잭션은 전후에 정의된 무결성 규칙을 지켜야 한다. 
- `Isolation(격리성)` : 동시 트랜잭션 사이의 간섭을 격리 수준에 따라 제한한다.
- `Durability(지속성)` : 커밋이 성공한 결과는 장애 이후에도 유지되어야 한다.

`COMMIT`은 작업을 확정하고 `ROLLBACK`은 해당 트랜잭션의 미확정 변경을 취소한다. 다만 같은 트랜잭션 내부에서는 커밋 전에도 자신의 변경 내용을 읽을 수 있으며, 다른 트랜잭션에 보이는지는 격리 수준과 DB에 따라 달라진다.

<br>
<hr>

## 2. JDBC로 직접 관리

<br>

```java
connection.setAutoCommit(false); // 자동 커밋 해제

try {
    // 같은 Connection으로 출금과 입금 쿼리 실행
    connection.commit();    
} catch (Exception e) {
    connection.rollback();
    throw e;
} finally {
    connection.setAutoCommit(true);    
}
```

- `DataSource`로 연결을 가져오고, **동일한 Connection**으로 모든 SQL을 실행한다.
- `PreparedStatement`는 SQL 매개변수를 안전하게 바인딩해 실행한다.
- 자원 반납과 연결 상태 복구를 직접 책임져야 하므로 코드가 길어지기 쉽다.
- 이 예제의 JDBC 방식은 `DataSource.getConnection()`으로 연결을 직접 가져오는 **독립된 JDBC 트랜잭션**이다. Spring 관리 트랜잭션에 임의로 섞어 넣지 않는다.

<br>
<hr>

## 3. 프로그래밍 방식 : PlatformTransactionManager

<br>

```java
TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

try {
    // JdbcTemplate 기반 출금·입금
    transactionManager.commit(status);    
} catch (RuntimeException | Error e) {
    if (!status.isCompleted()) transactionManager.rollback(status);
    throw e;
}
```

- `PlatformTransactionManager`는 Spring의 트랜잭션 추상화이다.
- `TransactionStatus`는 현재 트랜잭션 상태를 나타낸다.
- 시작/종료 위치를 코드로 지정할 수 있지만, 경계 관리가 서비스 코드에 드러난다.
- `JdbcTemplate`은 Spring이 바인딩한 JDBC 연결을 사용해 트랜잭션에 참여한다.
- 공식 문서에서는 일반적인 명령형 프로그래밍 트랜잭션에 `TransactionTemplate` 사용도 권장한다. 이번에는 직접 **commit/rollback 비교**를 위해 manager API를 사용했다.

<br>
<hr>

## 4. 선언적 방식 : @Transactional

<br>

```java
@Transactional
public void transfer(TransferRequest request) {
    accountRepository.changeBalance(request.fromId(), -request.amount());
    accountRepository.changeBalance(request.toId(), request.amount());
}
```

Spring은 보통 AOP 프록시를 통해 메서드 호출을 가로채 트랜잭션을 시작하고, 정상 반환 시 커밋한다. 기본 설정에서는 `RuntimeException`과 `Error`에 롤백하고 일반적인 체크 예외에는 롤백하지 않는다. (`rollbackFor`로 변경 가능)

- 프록시를 거치지 않는 **같은 객체 내부 메서드 호출(self-invocation)**은 별도 `@Transactional`을 적용하지 못할 수 있다.
- `@Transactional(readOnly = true)`는 읽기 작업에 대한 최적화 힌트이며, 모든 DB에서 쓰기를 강제로 차단하는 보안 장치는 아니다.
- 트랜잭션 범위는 보통 DB 변경을 조합하는 **Service 계층**에서 정한다.

<br>
<hr>

## 5. 세 방식 비교

<br>

<table>
    <thead>
        <th>방식</th>
        <th>경계 설정</th>
        <th>장점</th>
        <th>유의점</th>
    </thead>
    <tbody>
        <tr>
            <td>JDBC 직접 관리</td>
            <td>Connection의 commit/rollback</td>
            <td>JDBC 동작을 직접 관찰</td>
            <td>연결·예외·반납 코드가 길어짐</td>
        </tr>
        <tr>
            <td>프로그래밍 방식</td>
            <td>PlatformTransactionManager</td>
            <td>경계를 코드로 세밀하게 제어</td>
            <td>명시적인 완료/예외 처리 필요</td>
        </tr>
        <tr>
            <td>선언적 방식</td>
            <td>@Transactional</td>
            <td>비즈니스 로직에 집중</td>
            <td>프록시 호출 구조, 롤백 규칙 이해 필요</td>
        </tr>
    </tbody>
</table>

<br>