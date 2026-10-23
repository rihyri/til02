<br>

# Week 06 - Day03. Spring Boot로 확인하는 트랜잭션 격리 수준

<br>

## 1. 오늘 학습한 내용

<br>

어제는 MySQL에서 SQL을 직접 실행하며 트랜잭션의 격리 수준을 확인했다. 오늘은 Spring Boot의 `@Transactional`과 두 개의 실행 흐름을 이용해 같은 현상을 Java 코드에서 재현했다. 예제는 `Workshop`의 신청 인원을 수정하거나 조건에 맞는 강의를 추가하는 방식으로 구성했다.

<br>
<hr>

## 2. 실습 구조

<br>

- `Workshop` : 강의명과 신청 인원을 저장하는 JPA 엔티티
- `WorkshopRepository` : ID 조회와 신청 인원 기준 범위 조회
- `WorkshopIsolationService` : 트랜잭션별 읽기·수정·롤백 처리
- `WorkshopIsolationServiceTest` : 별도 스레드에서 트랜잭션 실행 순서 검증

한 스레드가 첫 번째 SQL을 실행한 후 `CountDownLatch`에서 대기하면, 다른 스레드가 데이터를 변경하거나 조회한 다음 첫 스레드를 다시 진행시킨다. 이를 통해 트랜잭션 사이에서 벌어지는 일을 의도적으로 재현할 수 있다.

<br>
<hr>

## 3. Dirty Read - 커밋되지 않은 값을 읽는 문제

<br>

A가 기존 신청 인원 20을 99로 수정하고 `flush()`로 SQL을 DB에 전달한다. 아직 커밋하지 않은 상태에서 B가 조회한다.

```java
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public int readUncommitted(Long id) {
    return workshopRepository.findById(id)
            .orElseThrow().getEnrolledCount();
}
```

- `READ_UNCOMMITTED`: B가 아직 확정되지 않은 99를 읽을 수 있다.
- `READ_COMMITTED` : B는 커밋된 기존 값인 20을 읽는다.
- 이후 A가 예외로 롤백하면 DB에는 최종적으로 20이 남는다.

`flush()`는 SQL을 DB에 반영하도록 전달하는 과정이지 `commit()`과 동일하지 않다. 또한 기본적으로 Spring은 `RuntimeException` 발생 시 트랜잭션을 롤백한다.

<br>
<hr>

## 4. Non-repeatable Read - 같은 행을 다시 읽었는데 값이 달라지는 문제

<br>

A가 신청 인원 20을 조회한다. 그 사이 B가 값을 7로 수정하고 커밋한다. A가 같은 강의를 다시 조회했을 때 결과를 비교한다.

```java
int first = repository.findById(id).orElseThrow().getEnrolledCount();
firstRead.countDown();
await(allowSecondRead);
entityManager.clear();
int second = repository.findById(id).orElseThrow().getEnrolledCount();
```

<br>

<table>
    <thead>
        <th>격리 수준</th>
        <th>첫 번째 조회</th>
        <th>두 번째 조회</th>
    </thead>
    <tbody>
        <tr>
            <td>READ COMMITTED</td>
            <td>20</td>
            <td>7</td>
        </tr>
        <tr>
            <td>REPEATABLE_READ (MySQL InnoDB)</td>
            <td>20</td>
            <td>20</td>
        </tr>
    </tbody>
</table>

<br>

JPA의 1차 캐시에 기존 엔티티가 남아 있으면 DB 재조회 없이 같은 값을 얻을 수 있다. 따라서 실습에서는 `EntityManager.clear()`를 호출해 1차 캐시를 비운다. 다만 이것이 DB의 MVCC 스냅샷을 초기화하지 않는다.

<br>
<hr>

## 5. Phantom Read - 조회 조건에 해당하는 행이 달라지는 문제

<br>

A가 `enrolledCount > 10` 조건의 강의 개수를 조회한다. 이후 B가 조건에 해당하는 강의를 새로 등록하고 커밋한다.

```java
long first = repository.countByEnrolledCountGreaterThan(10);
firstRead.countDown();
await(allowSecondRead);
long second = repository.countByEnrolledCountGreaterThan(10);
```

- `READ_COMMITTED`: 새로 커밋한 행을 포함하므로 개수가 1에서 2로 증가한다.
- `REPEATABLE_READ`: MySQL InnoDB의 일반적인 일관된 읽기에서는 최초 스냅샷을 사용하여 1개가 유지된다.
- `SERIALIZABLE` : 잠금 기반 제약이 강화되며 다른 트랜잭션의 삽입이 대기하거나 충돌할 수 있다.

표준적인 격리 수준 설명만으로 MySQL의 모든 결과를 단정해서는 안 된다. InnoDB에서는 일반 SELECT와 `SELECT ... FOR UPDATE` 등 잠금 읽기를 구분해야 한다.

<br>
<hr>

## 6. 코드에서 중요했던 부분

```java
@Transactional(isolation = ...)
```

트랜잭션의 DB 격리 수준을 지정한다. 트랜잭션 프록시를 통해 호출된 메서드에 적용되며, 같은 객체 내부에서 메서드를 직접 호출한다고 새 트랜잭션이 시작되는 것은 아니다. 

<br>

#### CountDownLatch
- `countDown()` : 작업 완료 신호를 보낸다.
- `await()` : 신호가 올 때까지 기다린다.

정해진 시간을 기다리는 `Thread.sleep()` 보다 테스트 단계 간 실행 순서를 명확하게 만들 수 있다. 단, 실제 DB의 락 획득 순서와 실행 성공까지 완전히 보장하지는 않는다.

<br>

### 테스트에서 `@Transactional`을 붙이지 않은 이유

테스트 메서드의 트랜잭션과 별도 스레드의 트랜잭션을 혼동하지 않기 위해서이다. 실습용 데이터는 테스트 준비 단계에서 저장·커밋하고, 각 Service 메서드가 자기 트랜잭션을 시작하도록 한다. 

<br>
<hr>

## 7. 오늘의 정리

<br>

어제는 SQL에서 격리 수준별 결과를 확인했다면 오늘은 `@Transactional`, JPA 영속성 컨텍스트, 별도 스레드가 이 결과에 어떤 영향을 주는지 확인했다. <br>
중요한 것은 어노테이션 이름을 외우는 것보다 **첫 번째 트랜잭션이 어디서 기다리고, 두 번째 트랜잭션이 언제 커밋하며, 첫 번째 트랜잭션의 조회가 무엇을 볼 수 있는지** 설명하는 것이다.

<br>

