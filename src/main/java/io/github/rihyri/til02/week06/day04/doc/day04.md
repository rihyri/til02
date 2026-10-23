<br>

# Week06 - Day04. 데이터베이스 Lock과 동시성 제어

<br>

## 1. Lock이란?

<br>

`Lock`은 여러 트랜잭션이 동일한 데이터에 접근할 때 발생할 수 있는 충돌을 제어하기 위한 데이터베이스의 동시성 제어 기능이다.

여러 요청이 동시에 발생하는 서비스에서는 하나의 데이터가 여러 트랜잭션에 의해 조회되거나 변경될 수 있다.

이때 적절한 동시성 제어가 없다면 데이터의 일관성이 깨지거나 기존 변경 사항이 사라질 수 있다.

예를 들어 공연의 좌석이 10석인 상태에서 두 사용자가 동시에 7석씩 예약을 요청했다고 가정해보자.

두 트랜잭션이 모두 잔여 좌석을 10석으로 조회하고 각각 예약을 처리한다면, 실제 가능한 좌석보다 많은 예약이 처리되는 문제가 발생할 수 있다.

이러한 상황을 방지하기 위해 Lock과 같은 동시성 제어 기법을 사용한다.

### Lock이 필요한 이유

- `데이터 정합성` : 여러 요청이 동시에 발생하더라도 올바른 데이터 상태를 유지한다.
- `동시성 제어` : 동일한 데이터에 대한 충돌하는 작업의 실행을 조정한다.
- `변경 유실 방지` : 다른 트랜잭션의 수정 결과가 잘못 덮어써지는 것을 방지한다.

Lock의 범위와 동작 방식은 데이터베이스 종류, 스토리 엔진, 트랜잭션 격리 수준에 따라 달라질 수 있다.

<br>
<hr>

## 2. Shared Lock과 Exclusive Lock

<br>

### Shared Lock (S-Lock)

공유 락은 여러 트랜잭션이 동일한 데이터를 읽을 수 있도록 허용하지만, 충돌하는 데이터 변경을 제한하는 락이다.

MySQL에서는 `FOR SHARE`를 통해 명시적으로 공유 락을 요청할 수 있다.

```SQL
START TRANSACTION;

SELECT * FROM concerts WHERE id = 1 FOR SHARE;

COMMIT;
```

공유 락을 보유하고 있는 동안 다른 트랜잭션이 같은 데이터에 공유 락을 획득하는 것은 가능하다. 

반면 해당 데이터를 변경하기 위해 배타적 락을 획득하려는 작업은 대기할 수 있다.

<br>

### Exclusive Lock (X-Lock)

배타적 락은 특정 데이터에 대한 충돌하는 읽기 잠금 및 쓰기 작업을 제한한다.

```SQL
SELECT * FROM concerts WHERE id = 1 FOR UPDATE;

COMMIT;
```

`FOR UPDATE`는 조회한 데이터를 이후에 수정할 가능성이 있을 때 사용하는 잠금 조회이다.

다른 트랜잭션에서 같은 행에 대해 `FOR UPDATE`를 실행하면 기존 트랜잭션이 락을 해제할 때까지 대기할 수 있다.

단, InnoDB의 일반적인 SELECT는 MVCC를 이용하므로 배타적 락이 있다고 해서 모든 읽기 작업이 차단되는 것은 아니다.

<br>

### IS-Lock과 IX-Lock

Intention Lock은 테이블 내부의 일부 행에 락을 설정하려는 의도를 나타내는 테이블 수준의 락이다.

- `IS (Intention Shared)` : 일부 행에 공유 락을 사용하려는 의도
- `IX (Intention Exclusive)` : 일부 행에 배타적 락을 사용하려는 의도

예를 들어 특정 행에 X-Lock을 설정하면, InnoDB는 해당 테이블에 IX-Lock을 함께 사용할 수 있다.

이 락들은 행 수준 락과 테이블 수준 락이 충돌하는지 판단하는 데 사용된다.

일반적으로 개발자가 직접 설정하기보다는 InnoDB가 내부적으로 관리한다.

<br>
<hr>

## 3. MySQL과 InnoDB의 Lock

<br>

MySQL에서는 서버 수준의 락과 InnoDB 스토리지 엔진 수준의 락을 구분할 필요가 있다.

<br>

### MySQL 서버 수준의 락

<table>
    <thead>
        <th>종류</th>
        <th>특징</th>
    </thead>
    <tbody>
        <tr>
            <td>Global Lock</td>
            <td>서버 전반의 작업에 영향을 주는 전역 락</td>
        </tr>
        <tr>
            <td>Table Lock</td>
            <td>특정 테이블에 대한 접근을 제어</td>
        </tr>
        <tr>
            <td>Metadata Lock</td>
            <td>테이블 구조의 변경과 사용 간 충돌을 방지</td>
        </tr>
    </tbody>
</table>

특히 Metadata Lock은 DDL 작업과 관련이 있다.

테이블을 사용하는 트랜잭션이 종료되지 않으면 `ALTER TABLE`과 같은 작업이 대기할 수 있다.

<br>

### InnoDB 수준의 락

<table>
    <thead>
        <th>종류</th>
        <th>특징</th>
    </thead>
    <tbody>
        <tr>
            <td>Record Lock</td>
            <td>인덱스의 특정 레코드에 설정되는 락</td>
        </tr>
        <tr>
            <td>Gap Lock</td>
            <td>인덱스 레코드 사이의 간격에 설정되는 락</td>
        </tr>
        <tr>
            <td>Next-Key Lock</td>
            <td>Record Lock과 Gap Lock을 결합한 형태</td>
        </tr>
        <tr>
            <td>Intention Lock</td>
            <td>테이블 내 행 잠금 의도를 나타내는 락</td>
        </tr>
    </tbody>
</table>

Gap Lock과 Next-Key Lock은 특정 범위에 새로운 레코드가 삽입되는 것을 제한하는 데 활용한다.

이러한 잠금은 트랜잭션 격리 수준과 검색 조건, 인덱스 사용 방식에 영향을 받는다.

따라서 `FOR UPDATE`를 사용했다고 해서 언제나 정확히 한 행만 잠기는 것은 아니다.

<br>
<hr>

## 4. Lost Update 

<br>

`Lost Update`는 여러 트랜잭션이 동일한 데이터를 수정하는 과정에서 일부 변경 내용이 다른 변경으로 덮어써지는 문제이다.

예를 들어 잔여 좌석이 10석인 공연을 두 트랜잭션이 동시에 조회했다고 가정하자.

```aiignore
1. 트랜잭션 A가 잔여 좌석 10석을 조회한다.
2. 트랜잭션 B가 잔여 좌석 10석을 조회한다.
3. A가 2석을 예약하고 잔여 좌석을 8석으로 변경한다.
4. B가 기존 조회 결과를 기준으로 3석을 예약하고 잔여 좌석을 7석으로 변경한다.
```

두 요청이 모두 성공했지만 최종 잔여 좌석은 5석이 아니라 7석이 된다.

A의 변경 내용이 B의 변경 내용으로 덮어써진 것이다.

이 문제를 해결하기 위한 대표적인 방법으로 비관적 락과 낙관적 락을 사용할 수 있다.

<br>
<hr>

## 5. 비관적 락 (Pessimistic Lock)

<br>

비관적 락은 다른 트랜잭션과 충돌할 가능성이 있다고 가정하고, 데이터를 사용하기 전에 락을 획득하는 방식이다.

조회 시점에 데이터를 잠가 다른 트랜잭션의 충돌하는 작업을 대기시킨다.

### SQL 예시
```SQL
START TRANSACTION;

SELECT * FROM concerts WHERE id = 1 FOR UPDATE;

UPDATE concerts SET remaining_seats = remaining_seats - 2 WHERE id = 1;

COMMIT;
```

`FOR UPDATE`를 통해 락을 획득한 뒤, 같은 트랜잭션에서 데이터 변경을 수행한다.

트랜잭션이 커밋되거나 롤백되면 락이 해제된다.

### JPA에서 비관적 락 사용

```JAVA
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Concert c WHERE c.id = :id")
Optional<Concert> findByIdWithLock(@Param("id") Long id);
```

`PESSIMISTIC_WRITE`는 쓰기 작업을 보호하기 위한 배타적 잠금 방식이다.

서비스에서는 `@Transactional`을 사용하여 조회부터 변경까지 하나의 트랜잭션으로 처리해야 한다.

### 특징

- 데이터 변경 전에 충돌을 제어할 수 있다.
- 동시 수정 요청이 많은 경우에 고려할 수 있다.
- 락 대기 때문에 처리 시간이 증가할 수 있다.
- 여러 자원을 서로 다른 순서로 잠그면 데드락이 발생할 수 있다.

<br>
<hr>

## 6. 낙관적 락 (Optimistic Lock)

<br>

낙관적 락은 데이터 충돌이 자주 발생하지 않을 것이라고 가정하고, 변경 시점에 충돌 여부를 검사하는 방식이다.

대표적으로 Version 컬럼을 이용한다.

### Version 컬럼

```java
@Version
private Long version;
```

JPA의 `@Version`은 Entity의 버전을 관리하고 다른 트랜잭션의 변경 여부를 확인하는 데 사용된다.

예를 들어 공연 데이터의 현재 버전이 0인 경우 다음과 같은 SQL을 생각할 수 있다.

```java
UPDATE concerts SET remaining_seats = 8, version = 1
    WHERE id = 1 AND version = 0
```

다른 트랜잭션이 이미 버전을 변경했다면 WHERE 조건에 맞는 행이 없어지므로 충돌이 감지된다.

<br>

### JPA에서 낙관적 락 사용

```java
@Lock(LockModeType.OPTIMISTIC)
@Query("SELECT c FROM Concert c WHERE c.id = :id")
Optional<Concert> findByIdWithVersion(@Param("id") Long id);
```

`@Version`이 선언된 Entity라면 일반적인 JPA 변경 감지에서도 버전 기반 충돌 검사가 적용될 수 있다.

명시적인 `OPTIMISTIC` 락은 트랜잭션 내에서 해당 Entity의 버전 확인을 요청하는 역할을 한다.

<br>

### 특징
- 미리 배타적인 락을 획득하지 않는다.
- 충돌이 적은 환경에서 불필요한 락 대기를 줄일 수 있다.
- 버전 충돌 시 트랜잭션이 실패할 수 있다.
- 서비스 정책에 따라 재시도 또는 사용자 안내가 필요하다.

<br>
<hr>

## 7. 비관적 락과 낙관적 락 비교

<br>

<table>
    <thead>
        <th>항목</th>
        <th>비관적 락</th>
        <th>낙관적 락</th>
    </thead>
    <tbody>
        <tr>
            <td>기본 접근</td>
            <td>충돌을 사전에 제어</td>
            <td>충돌을 사후에 감지</td>
        </tr>
        <tr>
            <td>주요 구현</td>
            <td>PESSIMISTIC_WRITE</td>
            <td>@Version</td>
        </tr>
        <tr>
            <td>DB 잠금</td>
            <td>명시적인 잠금 조회</td>
            <td>사전 잠금 조회 없이 버전 검사</td>
        </tr>
        <tr>
            <td>충돌 상황</td>
            <td>대기 또는 잠금 오류</td>
            <td>버전 충돌 및 롤백</td>
        </tr>
        <tr>
            <td>적합한 상황</td>
            <td>동시 수정이 빈번한 경우</td>
            <td>충돌이 드문 경우</td>
        </tr>
        <tr>
            <td>주의 상황</td>
            <td>락 대기, 데드락</td>
            <td>재시도, 충돌 처리</td>
        </tr>
    </tbody>
</table>

두 방식 중 항상 성능이 우수한 방법이 정해져 있는 것은 아니다.

동시 요청 규모, 충돌 빈도, 트랜잭션 실행 시간, 재시도 비용을 고려하여 선택해야 한다.

<br>
<hr>

## 8. 동시성 테스트

<br>

락은 단일 요청만 실행해서는 효과를 확인하기 어렵다.

따라서 여러 스레드를 사용하여 동일한 데이터에서 동시에 접근하는 테스트가 필요하다.

<br>

### ExecutorService

여러 작업을 병렬로 실행하기 위한 스레드 풀을 관리한다.

```java
ExecutorService executor =
        Executors.newFixedThreadPool(2);
```

### CountDownLatch

특정 작업들이 완료되거나 준비될 때까지 다른 스레드를 대기시키는 동기화 도구이다.

```java
CountDownLatch latch = new CountDownLatch(2);
```

각 스레드에서 `countDown()`을 실행하면 카운트가 감소하고, 카운트가 0이 되면 `await()`의 대기가 종료된다.

<br>

### 테스트에서 확인할 항목

- 여러 요청이 동시에 실행되는가?
- 한 요청이 락을 획득했을 때 다른 요청은 어떻게 동작하는가?
- 낙관적 락의 버전 충돌이 발생하면 어떻게 처리되는가?
- 최종 데이터가 예상한 상태로 유지되는가?

테스트 결과뿐 아니라 실행된 SQL과 트랜잭션의 흐름도 함께 확인하는 것이 중요하다.

<br>
<hr>

## 9. 학습 핵심 정리

<br>

이번 학습에서는 데이터베이스의 `Lock`을 통해 동시성 문제를 제어하는 방법을 확인했다.

비관적 락은 데이터를 사용하기 전에 충돌을 제어하고, 낙관적 락은 데이터를 변경할 때 버전을 확인하여 충돌을 감지한다.

JPA에서는 `@Lock`, `LockModeType`, `@Version`을 활용하여 두 가지 방법을 구현할 수 있다.

Lock을 사용할 때는 트랜잭션 범위와 실제 DB의 동작을 함께 이해해야 한다.

또한 동시성 문제는 단순한 단위 테스트만으로 확인하기 어려우므로, 여러 스레드가 동일한 데이터에 접근하는 상황을 직접 재현하는 것이 필요하다.

<br>

