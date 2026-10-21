<br>

# Week06 - Day02. 동시성 문제와 트랜잭션 격리 수준

<br>

> 실습 환경: MySQL 8.x / InnoDB, 터미널 A·B
> 
> 이 문서는 수업 자료의 흐름을 기반으로 SQL 재현 절차를 보완한 학습 정리이다. 일부 실습과 설명은 이해를 위해 추가했다.

<br>

## 1. 동시성 문제란?

<br>

**동시성**(Concurrency)은 여러 작업 또는 트랜잭션이 같은 시기에 데이터에 접근하는 상황이다.

서버에서는 여러 사용자의 요청을 동시에 처리해야 하므로 동시성 자체는 필수적이다. 문제는 **읽기·수정 타이밍이 겹치면서 예상치 못한 결과가 생기는 것**이다.

### 예시: 마지막 재고 10개에 주문 2건

- A가 재고 `10`을 읽고 `7`개 주문이 가능하다고 판단한다.
- B도 같은 재고 `10`을 읽고 `7`개 주문이 가능하다고 판단한다.
- 두 주문을 모두 허용하면 총 `14`개가 팔려 실제 재고가 부족해진다.
- 단순히 각각 `stock = 3`을 저장하면 두 주문이 처리되었어도 DB에는 재고가 `3`으로 남는 **갱신 손실(Lost Update)** 형태의 문제가 나타날 수 있다.

> 동시성 문제와 트랜잭션 격리 수준은 연결되어 있지만, **격리 수준을 높인다고 해서 모든 재고 차감 로직이 자동으로 안전해지는 것은 아니다.** 조건부 UPDATE, 잠금, 충돌 재시도 같은 별도 제어가 필요할 수도 있다.

<br>

### 동시성(Concurrency)와 병렬성(Parallelism)의 차이

동시성은 여러 작업이 **겹치는 시간 동안 진행되는 구조**를 뜻하고, 병렬성은 여러 작업이 **실제로 같은 순간에 실행되는 것**을 뜻한다. CPU 코어가 하나여도 작업을 번갈아 처리하면 동시성이 존재할 수 있다. DB 관점에서는 두 요청의 실행 구간이 겹치기만 해도 데이터 경합이 발생할 수 있다는 점이 중요하다.

<br>
<hr>

## 2. ACID - 트랜잭션의 네 가지 속성

<br>

<table>
    <thead>
        <th>속성</th>
        <th>의미</th>
        <th>예시</th>
    </thead>
    <tbody>
        <tr>
            <td>Atomicity (원자성)</td>
            <td>작업 전체가 성공하거나 전부 취소됨</td>
            <td>계좌 A 차감 후 B 입금 실패 시 차감 롤백</td>
        </tr>
        <tr>
            <td>Consistency (일관성)</td>
            <td>정의된 데이터 제약을 지킴</td>
            <td>재고가 음수가 되지 않도록 제약·로직 적용</td>
        </tr>
        <tr>
            <td>Isolation (격리성)</td>
            <td>동시 트랜잭션끼리 간섭하는 정도를 제어</td>
            <td>다른 거래의 미커밋 변경을 보지 않음</td>
        </tr>
        <tr>
            <td>Durability (지속성)</td>
            <td>커밋된 결과를 장애 뒤에도 보존</td>
            <td>완료된 주문 정보가 복구 후에도 남음</td>
        </tr>
    </tbody>
</table>

<br>

`START TRANSACTION`은 작업을 시작하고, `COMMIT`은 변경을 확정하며, `ROLLBACK`은 미확정 변경을 취소한다.

특히 **원자성**과 **격리성**은 서로 다른 속성이다. 롤백이 가능하다는 사실만으로 동시 요청 간 충돌이 해결되지는 않는다. 각각의 트랜잭션이 정상적으로 커밋되었더라도 비즈니스 규칙에 맞지 않는 결과가 나올 수 있기 때문에 별도의 동시성 제어가 필요하다.

```sql
START TRANSACTION;
UPDATE products SET stock = stock - 1 WHERE id = 1;
-- 검증에 성공한 경우
COMMIT;
-- 검증 실패였다면 COMMIT 대신 ROLLBACK;
```

<br>
<hr>

## 3. 실습 준비

<br>

**중요:** 터미널 A와 B는 같은 DB를 바라보지만 **서로 다른 커넥션**이어야 한다. A에서 `COMMIT`하지 않은 채 B로 이동하는 것이 핵심이다. 

각 실습은 아래 `UPDATE`로 **초깃값을 복원하고 기존 트랜잭션을 종료한 뒤** 별도로 진행한다.

<br>

```sql
CREATE TABLE IF NOT EXISTS products (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    stock INT NOT NULL
);

DELETE FROM products;

INSERT INTO products (id, name, stock) VALUES
    (1, '파란색 와이셔츠', 10),
    (2, '달콤한 버터감자', 25);
    
SELECT * FROM products ORDER BY id;
-- 1 | 파란색 와이셔츠 | 10
-- 2 | 달콤한 버터감자 | 25
```

> 초기화 쿼리는 **진행 중인 트랜잭션이 없는 상태에서** 실행한다. `DELETE`/`INSERT`가 다른 실습과 충돌하지 않게 주의한다.


<br>

### 터미널 A·B를 분리하는 이유

두 터미널에서 각각 MySQL에 접속하면 별도의 세션과 트랜잭션이 형성된다. 같은 터미널 안에서 쿼리만 순서대로 실행하면 다른 트랜잭션이 중간에 끼어드는 상황을 재현하기 어렵다.

**A에서 쿼리를 일부 실행 → B로 이동 → 다시 A로 이동**하는 순서가 실제로 동시 요청의 실행 순서를 흉내 내는 방법이다.

<br>

### 격리 수준 확인 · 설정

```sql
SELECT @@transaction_isolation;

-- A, B 각각 실행: 현재 연결의 '이후 트랜잭션'에 적용
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

SELECT @@transaction_isolation;
-- READ-COMMITTED
```

`SET GLOBAL ...`은 새로 생성되는 세션에 영향을 주는 서버 단위 설정으로, 실습에서는 다른 연결에 의도치 않은 영향을 줄 수 있으므로 **SESSION 설정을 권장**한다. 

격리 수준은 테스트마다 새 트랜잭션을 시작하기 **전에** 설정한다.

<br>
<hr>

## 4. 격리 수준 비교

<br>

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
            <td>MySQL InnoDB 일반 SELECT는 MVCC로 방지</td>
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

위 표의 `REPEATABLE READ` 부분은 **MySQL InnoDB의 일반적인 비잠금 SELECT(일관된 읽기)** 기준이다. SQL 표준의 일반적인 허용 현상과 실제 엔진 구현은 구분해야 한다. `SELECT ... FOR UPDATE, UPDATE, DELETE 등` **현재 읽기**(Current read)는 일반 SELECT의 스냅샷 읽기와 다르게 동작할 수 있다. 

격리 수준이 높을수록 일관성을 더 엄격하게 유지할 수 있지만, 잠금·대기·충돌이 늘어갈 가능성도 있다. 따라서 실제 서비스에서는 모든 트랜잭션에 최고 수준을 설정하기보다 **데이터 정합성 요구와 성능**을 함께 고려하여 선택한다.

<br>

### 4.1 READ UNCOMMITTED

아직 커밋되지 않은 변경 결과도 볼 수 있는 가장 낮은 수준이다. 다른 트랜잭션이 롤백하면 **실제로 확정되지 않은 값**을 읽은 셈이 된다.

### 4.2 READ COMMITTED

다른 트랜잭션의 **커밋된 값만** 읽는다. 다만 일반 SELECT를 실행할 때마다 새 스냅샷이 만들어지므로 같은 트랜잭션 내에서도 두 조회 결과가 달라질 수 있다.

### 4.3 REPEATABLE READ

MySQL InnoDB 기본 격리 수준이다. 일반적인 일관된 SELECT는 **첫 번째 일관된 읽기에서 생성된 스냅샷**을 이후에도 사용한다. `START TRANSACTION` 자체가 반드시 스냅샷 생성 시점인 것은 아니다.

### 4.4 SERIALIZABLE

가장 엄격한 수준으로 동시 변경을 제한한다. MySQL에서는 트랜잭션 안의 일반 SELECT도 조건을 따라 잠금 읽기처럼 처리하므로 다른 트랜잭션의 삽입·수정이 대기할 수 있다. 처리량과 잠금 경합을 고려해야 한다.

<br>
<hr>

## 5. 실습 ① Dirty Read - 커밋되지 않은 데이터 읽기

<br>

**상황 :** A가 재고를 10 → 5로 수정했지만 아직 커밋하지 않았다. B는 그 5를 읽고, 이후 A는 변경을 취소한다.

<br>

### 5.1 문제 재현 : READ UNCOMMITTED

**사전 준비:** A·B 각각 `SET SESSION ... READ UNCOMMITTED` 설정, 상품 1의 재고 `10` 확인

<br>

#### 💻 터미널 A
```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
START TRANSACTION;
UPDATE products SET stock = 5 WHERE id = 1;
SELECT stock FROM products WHERE id = 1;
-- 5: 아직 COMMIT하지 않음!
```

#### 💻 터미널 B
```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
START TRANSACTION;
SELECT stock FROM products WHERE id = 1;
-- 예상: 5 ← Dirty Read
```

#### 💻 터미널 A
```sql
ROLLBACK;
-- A의 5는 확정되지 않았고, 실제 값은 다시 10
```

#### 💻 터미널 B
```sql
SELECT stock FROM products WHERE id = 1;
-- 예상: 10
COMMIT;
```

**왜 문제인가?** B는 잠시 존재했던 `5`를 진짜 재고라고 판단했지만 A의 롤백으로 그 변경이 사라졌다. 해당 값으로 후속 업무를 처리했다면 오판할 수 있다.

예를 들어 재고가 5개라고 판단해 품절 알림을 보냈다면, 실제 재고가 다시 10개로 돌아오더라도 이미 발송된 알림은 자동으로 취소되지 않는다. DB 트랜잭션 바깥에서 일어난 후속 작업까지 생각해야 하는 이유다.

<br>

### 5.2 해결 확인: READ COMMITTED

실습 전 `stock=10`으로 되돌리고 두 세션의 트랜잭션이 종료된 상태인지 확인한다.

<br>

#### 💻 터미널 A
```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
UPDATE products SET stock = 5 WHERE id = 1;
-- 아직 COMMIT 하지 않음
```

#### 💻 터미널 B
```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SELECT stock FROM products WHERE id = 1;
-- 예상: 10 (A의 미커밋 값 5는 보지 못함)
```

#### 💻 터미널 A
```sql
COMMIT;
```

#### 💻 터미널 B
```sql
SELECT stock FROM products WHERE id = 1;
-- 예상: 5 (이제 커밋된 값)
COMMIT;
```

**핵심 :** READ COMMITTED는 Dirty Read는 방지하지만, B의 두 SELECT 결과가 `10 → 5`로 달라지는 것은 허용한다. 이 현상이 다음 실습이다.

<br>
<hr>

## 6. 실습 ② Non-Repeatable Read - 같은 행을 다시 읽었는데 값이 달라짐

<br>

**상황 :** A가 재고 10을 읽고 작업 중인데, B가 5로 수정·커밋한다.

<br>

### 6.1 문제 재현: READ COMMITTED

**초기화 :** 트랜잭션 밖에서 `UPDATE products SET stock = 10 WHERE id = 1;`을 실행한다.

<br>

#### 💻 터미널 A
```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SELECT stock FROM products WHERE id = 1;
-- 첫 조회: 10
```

#### 💻 터미널 B
```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
UPDATE products SET stock = 5 WHERE id = 1;
COMMIT;
```

#### 💻 터미널 A
```sql
SELECT stock FROM products WHERE id = 1;
-- 두 번째 조회: 5 (Non-Repeatable Read)
COMMIT;
```

**핵심 :** 동일한 행에 대한 같은 SELECT인데, B의 커밋 전후로 결과가 바뀐다.

<br>

### 6.2 해결 확인: REPEATABLE READ

**초기화 :** 재고를 다시 10으로 되돌린다.

#### 💻 터미널 A
```sql
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;
SELECT stock FROM products WHERE id = 1;
-- 첫 조회: 10 (일관된 읽기 스냅샷 생성)
```

#### 💻 터미널 B
```sql
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;
UPDATE products SET stock = 5 WHERE id = 1;
COMMIT;
-- DB의 최신 커밋 값은 5
```

#### 💻 터미널 A
```sql
SELECT stock FROM products WHERE id = 1;
-- 두 번째 일반 SELECT: 여전히 10
COMMIT;

SELECT stock FROM products WHERE id = 1;
-- 새 트랜잭션/조회에서는 5
```

**왜 10이 보일까?** A의 기존 스냅샷은 10인 시점을 가리키기 때문이다. 

**B의 업데이트가 차단된 것은 아니다.** B는 커밋했고, A의 일반 SELECT만 이전 버전을 읽는다.

<br>
<hr>

## 7. 실습 ③ Phantom Read - 조건에 맞는 행이 늘어남

<br>

**Non-Repeatable Read**는 특정 행의 **값**이 바뀌는 현상이고, **Phantom Read**는 `WHERE` 조건으로 읽은 **행 집합**이 달라지는 현상이다.

예를 들어 `WHERE id = 1`로 한 상품의 재고를 두 번 읽다가 값이 달라졌다면 전자에 가깝다. 반면 `WHERE stock > 10`으로 조회한 상품 목록에 새 상품이 추가되어 결과 행 수가 달라졌다면 후자에 해당한다. 두 현상을 구분하는 기준이 단순히 `UPDATE`와 `INSERT`의 차이가 아니라 **반복 조회에서 관찰한 결과의 성격**이다.

<br>

### 7.1 READ COMMITTED에서 재현

**초기화 :** `products`에는 `(1, 와이셔츠, 10)`, `(2, 버터감자, 25)`만 남긴다. 조건을 `stock > 10`으로 설정하면 **처음 결과는 id=2의 1행**이다.

<br>

#### 💻 터미널 A
```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SELECT id, name, stock FROM products WHERE stock > 10 ORDER BY id;
-- 처음: id=2 (1행)
```

#### 💻 터미널 B
```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
INSERT INTO products (id, name, stock) VALUES (3, '신상품', 15);
COMMIT;
```

#### 💻 터미널 A
```sql
SELECT id, name, stock FROM products WHERE stock > 10 ORDER BY id;
-- 두 번째: id=2, id=3 (2행) → Phantom Read
COMMIT;
```

<br>

### 7.2 SERIALIZABLE로 삽입 대기 확인

**초기화 :** id=3 삭제 후 시작한다. 두 터미널 모두 새 트랜잭션을 사용한다.

<br>

#### 💻 터미널 A
```sql
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT id FROM products WHERE stock > 10;
-- id=2: 트랜잭션 내 잠금 읽기 처리
-- COMMIT하지 않은 채 B로 이동
```

#### 💻 터미널 B
```sql
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;
SET SESSION innodb_lock_wait_timeout = 30;
START TRANSACTION;
INSERT INTO products (id, name, stock) VALUES (3, '신상품', 15);
-- A가 잠금을 보유하는 동안 대기할 수 있음
-- 타임아웃이 먼저 나면 1205 오류가 날 수 있음
```

#### 💻 터미널 A
```sql
COMMIT;
-- 잠금 해제 → B의 INSERT가 성공하며 진행할 수 있음
```

#### 💻 터미널 B
```sql
-- INSERT 성공 후 (오류가 나지 않았다면)
COMMIT;
```

> 잠금 대기는 무한히 지속된다고 단정할 수 없다. **잠금 대기 타임아웃**, 데드락, 인덱스 구성과 실행 계획의 영향도 받는다. 오류가 발생하면 B에서 `ROLLBACK` 후 정리한다.

<br>
<hr>

## 8. MVCC (Multi-Version Concurrency Control) 이해하기

<br>

MVCC는 데이터의 **여러 버전**을 활용해서 읽기 작업과 쓰기 작업의 충돌을 줄이는 방식이다.

InnoDB는 Undo Log 및 트랜잭션의 **Read View**를 활용해 과거 버전을 재구성하여 일관된 읽기를 제공한다.

- **Undo Log :** 데이터 변경 이전 상태를 복원하거나 과거 버전 조회에 활용되는 정보를 보관한다.
- **Read View** : 현재 트랜잭션에서 어떤 변경 사항을 볼 수 있는지 판단하는 기준이다.
- **일관된 읽기(Consistent Read) :** 잠금을 획득하지 않는 일반 `SELECT`가 적절한 버전을 읽는 방식이다.

즉, MVCC는 모든 데이터를 별도 테이블에 복사해 저장하는 기능이 아니라, 트랜잭션별로 **보여야 하는 데이터 버전**을 결정하는 메커니즘이다.

<table>
    <thead>
        <th>시점</th>
        <th>실제 변경</th>
        <th>A의 일반 SELECT (REPEATABLE READ)</th>
    </thead>
    <tbody>
        <tr>
            <td>①</td>
            <td>최초 커밋 값 stock = 10</td>
            <td>10 (첫 스냅샷)</td>
        </tr>
        <tr>
            <td>②</td>
            <td>B가 stock=5로 변경하고 COMMIT</td>
            <td>아직 조회 안 함</td>
        </tr>
        <tr>
            <td>③</td>
            <td>최신 DB값은 5</td>
            <td>10 (기존 스냅샷 유지)</td>
        </tr>
        <tr>
            <td>④</td>
            <td>A가 COMMIT 후 다시 조회</td>
            <td>5 (새 스냅샷)</td>
        </tr>
    </tbody>
</table>

<br>

### 8.1 직접 보는 MVCC 실습 - A와 B가 서로 다른 값을 읽기

**초기화 :** 재고 10, 양쪽 트랜잭션 종료한다.

<br>

#### 💻 터미널 A
```sql
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;
SELECT stock FROM products WHERE id = 1;
-- 10 : 이 시점에서 A의 일관된 읽기 스냅샷 생성
```

#### 💻 터미널 B
```sql
START TRANSACTION;
UPDATE products SET stock = 5 WHERE id = 1;
COMMIT;
SELECT stock FROM products WHERE id = 1;
-- 5 : B는 최신 커밋 값 확인
```

#### 💻 터미널 A
```sql
SELECT stock FROM products WHERE id = 1;
-- 10 : A는 스냅샷의 과거 버전을 봄
COMMIT;
SELECT stock FROM products WHERE id = 1;
-- 5 : 트랜잭션이 끝났으므로 최신 커밋 결과가 보임
```

**중요한 세부 사항**

1. `START TRANSACTION` 시점보다 **첫 일반 SELECT** 시점이 기본 REPEATABLE READ의 Read View 형성에 중요하다. (예외적으로 `START TRANSACTION WITH CONSISTENT SNAPSHOT`은 시작 시점 스냅샷을 요청한다.)
2. 읽기 가능한 버전은 단순히 "내 트랜잭션 ID보다 작은 ID"로 결정되지 않는다. **활성 트랜잭션 목록·커밋 가시성**을 고려하는 Read View 규칙을 따른다.
3. 같은 트랜잭션에서 **자신이 직접 변경한 내용**은 일반적으로 SELECT에서도 볼 수 있다.
4. `SELECT ... FOR UPDATE`는 일반 스냅샷 조회와 다르다. **잠금을 획득하는 현재 읽기**다.

<br>

### 8.2 추가 실습 - 일반 SELECT vs FOR UPDATE

자료의 MVCC 질문을 확장한 예제이다. 결과 차이를 가장 직관적으로 비교할 수 있다.

**초기화 :** 재고 10, 양쪽 트랜잭션 종료한다.

<br>

#### 💻 터미널 A
```sql
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;
SELECT stock FROM products WHERE id = 1;
-- 10: A의 스냅샷 생성
```

#### 💻 터미널 B
```sql
START TRANSACTION;
UPDATE products SET stock = 7 WHERE id = 1;
COMMIT;
-- DB 최신 커밋 값 7
```

#### 💻 터미널 A
```sql
SELECT stock FROM products WHERE id = 1;
-- 10: 일반 SELECT는 기존 스냅샷

SELECT stock FROM products WHERE id = 1 FOR UPDATE;
-- 7: 잠금 읽기는 최신 커밋 버전을 대상으로 함
-- (다른 트랜잭션이 해당 행 잠금을 보유 중이라면 대기 가능)

COMMIT;
```

**왜 중요한가?** "REPEATABLE READ에서는 항상 오래된 값만 보인다"는 설명은 정확하지 않다. **조회 종류에 따라 다르다.**

<br>
<hr>

## 9. 추가 응용 - 안전한 재고 차감은 어떻게 할까?

<br>

> 수업 자료의 재고 부족 상황을 조금 더 확장한 응용 예제다.

<br>

### 9.1 위험한 방식: 읽고 계산한 값을 그대로 저장

```SQL
-- A와 B 동시에 stock = 10을 읽었다고 가정
SELECT stock FROM products WHERE id = 1;

-- 애플리케이션에서 10 - 7 = 3 계산
UPDATE products SET stock = 3 WHERE id = 1;
-- 두 요청 모두 같은 값 3을 기록하면 하나의 차감이 유실될 수 있음
```

<br>

### 9.2 방법 A: 조건부 UPDATE 한 문장으로 검증 + 차감

```SQL
UPDATE products
    SET stock = stock - 7
WHERE id = 1 AND stock >= 7;

SELECT ROW_COUNT() AS updated_rows;
-- 1: 차감 성공
-- 0: 대상이 없거나 남은 재고가 부족함
```

**왜 안전한가?** 조건 검사와 차감을 **하나의 UPDATE로** 처리해, 두 트랜잭션이 같은 초기 재고를 확인한 뒤 모두 성공 처리하는 문제를 피할 수 있다. 

두 요청이 각각 7개를 차감하려는 경우, 첫 번째가 성공해 재고 3이 되면 두 번째는 조건을 만족하지 않아 영향 행 수가 0이 된다. 실제 API에서는 `updated_rows=0`을 주문 실패로 처리하고 적절히 롤백해야 한다.

<br>

### 9.3 방법 B: 잠금 읽기 후 수정

```SQL
START TRANSACTION;

SELECT stock FROM products WHERE id = 1 FOR UPDATE;
-- 읽은 행에 배타적인 잠금을 확보하여 경쟁 수정이 대기하게 함

-- 애플리케이션에서 stock >= 요청 수량 검증 후:
UPDATE products SET stock = stock - 7 WHERE id = 1;
COMMIT;
```

`FOR UPDATE`를 사용하더라도 **검증을 생략하면 안 된다.** 잠금 획득 후 실제 읽은 재고를 기준으로 남은 수량을 검증해야 한다. 

<br>

### 9.4 조건부 UPDATE와 FOR UPDATE는 언제 사용할까?

조건과 변경을 하나의 SQL로 표현할 수 있는 단순 재고 차감에서는 **조건부 UPDATE**가 상대적으로 간결하다. 반면 여러 값을 조회하고 복잡한 검증을 거쳐 수행해야 한다면 `SELECT ... FOR UPDATE`처럼 잠금을 명시적으로 사용하는 방식을 검토할 수 있다. 어느 방식이든 트랜잭션 범위, 실패 처리, 잠금 경합 가능성까지 함께 설계해야 한다. 

<br>
<hr>

## 10. 핵심 비교 및 복습

<br>

<table>
    <thead>
        <th>이슈</th>
        <th>핵심 질문</th>
        <th>대표 재현 수준</th>
        <th>비교·방어 포인트</th>
    </thead>
    <tbody>
        <tr>
            <td>Dirty Read</td>
            <td>커밋 전 값을 읽었나?</td>
            <td>READ UNCOMMITTED</td>
            <td>READ COMMITTED</td>
        </tr>
        <tr>
            <td>Non-Repeatable Read</td>
            <td>같은 행의 값이 바뀌었나?</td>
            <td>READ COMMITTED</td>
            <td>REPEATABLE READ 일반 SELECT</td>
        </tr>
        <tr>
            <td>Phantom Read</td>
            <td>같은 조건의 행 집합이 바뀌었나?</td>
            <td>READ COMMITTED</td>
            <td>InnoDB 일반 SELECT 스냅샷</td>
        </tr>
    </tbody>
</table>

<br>

