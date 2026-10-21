
<br>

# Week 06 - Day 02. 동시성 문제 및 트랜잭션 복습 · 회고

<br>

이번 수업에서는 여러 사용자가 같은 데이터를 동시에 조회·수정할 때 **동시성 문제**가 발생할 수 있다는 것을 학습했다. 트랜잭션의 ACID 속성과 네 가지 격리 수준을 정리하고, MySQL에서 두 개의 세션을 사용해 **Dirty Read, Non-Repeatable Read, Phantom Read**를 확인하는 SQL 실습 흐름을 살펴보았다.

<br>

### 핵심 개념

### 1. Dirty Read
- **상황 :** A가 재고를 10 → 5로 변경하고 아직 COMMIT하지 않았는데 B가 5를 읽는다.
- **문제 :** A가 ROLLBACK하면 B가 읽은 5는 확정되지 않은 값이 된다.
- **정리 :** `READ UNCOMMITTED`에서 발생할 수 있으며, `READ COMMITTED` 이상에서는 방지된다.

### 2. Non-Repeatable Read
- **상황 :** A가 재고 10을 읽는 동안 B가 재고를 5로 변경·커밋한다. A가 다시 읽으면 5가 보인다.
- **문제 :** 같은 트랜잭션 안에서 **같은 행의 값**이 달라진다.
- **정리 :** `READ COMMITTED`에서는 허용되며, MySQL `REPEATABLE READ`의 일반 SELECT는 스냅샷을 유지하여 방지한다.

### 3. Phantom Read
- **상황 :** A가 `stock > 10`으로 조회한 뒤 B가 조건에 맞는 상품을 새로 INSERT하고 COMMIT한다.
- **문제 :** A가 다시 조회했을 때 **조건에 맞는 행의 개수**가 달라진다.
- **정리 :** MySQL `READ COMMITTED`에서 실습할 수 있다. 단, **InnoDB REPEATABLE READ의 일반 SELECT**는 MVCC 스냅샷을 사용해 같은 트랜잭션의 조회 결과를 유지한다.

### 4. MVCC
- **여러 버전의 데이터를 활용해** 읽기와 쓰기의 충돌을 줄이는 기술이다.
- MySQL InnoDB는 Undo Log와 Read View를 이용해 읽기에 필요한 과거 버전을 재구성한다.
- `REPEATABLE READ`에서 기본적인 일반 SELECT의 스냅샷은 **첫 일관된 읽기** 때 만들어진다.
- `SELECT ... FOR UPDATE`처럼 **잠금을 거는 읽기**는 일반 스냅샷 SELECT와 다르다.

### Q1. `START TRANSACTION`을 실행하면 무조건 그 순간 스냅샷이 만들어질까?
일반적인 InnoDB `REPEATABLE READ`에서는 **첫 일관된 읽기 SELECT**가 기준이다. 예외적으로 `WITH CONSISTENT SNAPSHOT` 구문을 사용할 수 있다.

### Q2. `REPEATABLE READ`는 다른 트랜잭션의 UPDATE를 막아주는 걸까?
그렇지 않는다. 일반 SELECT는 이전 버전을 읽을 수 있으며, 다른 트랜잭션은 업데이트하고 커밋할 수 있다. 잠금 읽기와 구별해야 한다.

<br>

- 동시성을 높이는 것과 데이터의 정확성을 지키는 것 사이에는 균형이 필요하다.
- 트랜잭션 격리 수준은 **다른 트랜잭션의 변경을 언제 볼 수 있느냐**에 중요한 영향을 준다.
- MySQL은 **MVCC 기반 읽기**와 **잠금 기반 현재 읽기**를 구분해서 이해해야 한다.
- 실제 서비스에서는 이 개념을 단순히 외우기보다, **어떤 요청이 동시에 들어왔을 때 어떤 SQL이 실행되는지** 생각하는 습관이 중요하다.

<br>

