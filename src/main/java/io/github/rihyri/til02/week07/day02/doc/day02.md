<br>

# Week 07 - Day 02. EXPLAIN과 페이징 쿼리 최적화

> MySQL 실행 계획을 해석하고, 인덱스의 효과를 검증하며, `OFFSET/LIMIT`와 Keyset Pagination의 성능 및 정확성 차이를 이해한다.
> 
> 실습 환경: **MySQL 8.0.18 이상** (`EXPLAIN ANALYZE` 사용)

<br>

## 1. 실습 데이터베이스 준비

<br>

### 1.1 테이블 생성

이전 실습에서 데이터를 지운 상태이므로 **테이블부터 새로 생성**한다. 

```sql
CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    category_id INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

SHOW INDEX FROM products;
```

기본 키인 `id`에는 클러스터형 인덱스가 생성된다. 별도로 인덱스를 생성하기 전후를 비교하기 위해 다른 인덱스는 아직 추가하지 않았다.


<br>

### 1.2 100,000건의 실습 데이터 삽입
실행 계획 비교에서 작은 데이터셋은 최적화 차이가 잘 드러나지 않을 수 있다. 아래 데이터는 `created_at`에 **동일한 시간이 반복되도록** 만들었다. 

```sql
-- 동일 세션에서 실행: 기본 재귀 최대 깊이(1,000) 초과를 허용
SET SESSION cte_max_recursion_depth = 100001;

INSERT INTO products (name, description, price, stock, category_id, created_at, updated_at)
WITH RECURSIVE seq (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 100000
)

SELECT
    CONCAT('Product ', n),
    CONCAT('Sample product number ', n),
    CAST((1000 + (n % 5000) * 10) AS DECIMAL(10, 2)),
    n % 100,
    1 + (n % 1000),
    DATE_ADD('2025-01-01 00:00:00', INTERVAL (n % 1440) MINUTE),
    DATE_ADD('2025-01-01 00:00:00', INTERVAL (n % 1440) MINUTE)
FROM seq;

SELECT COUNT(*) AS total_count, MIN(id) AS min_id, MAX(id) AS max_id
FROM products;
```

<br>
<hr>

## 2. EXPLAIN이란?

`EXPLAIN`은 쿼리를 실행할 때 MySQL 옵티마이저가 선택한 **실행 계획**을 확인하는 도구이다. 데이터를 어떻게 접근하고, 몇 행을 확인할 것으로 예상하며, 어떤 인덱스를 활용하는지 살펴볼 수 있다.

```sql
EXPLAIN
SELECT id, name, price
FROM products
WHERE category_id = 100;
```

- `EXPLAIN` : 추정된 계획과 비용 정보를 확인한다.
- `EXPLAIN ANALYZE` : 실제 쿼리를 실행하면서 단계별 실제 수행 시간을 확인한다. (MySQL 8.0.18+) `SELECT`도 실제 읽기 작업을 수행하므로 비용이 큰 쿼리를 주의한다.

```sql
EXPLAIN ANALYZE
SELECT id, name, price
FROM products
WHERE category_id = 100;
```

<br>

### 2.1 주요 실행 계획 필드

<br>

<table>
    <thead>
        <th>필드</th>
        <th>의미</th>
        <th>확인 포인트</th>
    </thead>
    <tbody>
        <tr>
            <td>type</td>
            <td>테이블에 접근하는 방식</td>
            <td>ALL, index, range, ref, eq_ref, const, system 등</td>
        </tr>
        <tr>
            <td>possible_keys</td>
            <td>후보로 고려할 수 있는 인덱스</td>
            <td>후보가 있어도 실제 사용하지 않을 수 있다.</td>
        </tr>
        <tr>
            <td>key</td>
            <td>실제 선택한 인덱스</td>
            <td>NULL이면 그 접근에서 인덱스가 선택되지 않은 것이다.</td>
        </tr>
        <tr>
            <td>rows</td>
            <td>읽을 것으로 예측한 행 수</td>
            <td>실제로 읽은 건수가 아닌 추정치이다.</td>
        </tr>
        <tr>
            <td>filtered</td>
            <td>조건을 통과할 것으로 예측한 비율(%)</td>
            <td>두 값을 함께 보면 통과 행의 규모를 예상할 수 있다.</td>
        </tr>
        <tr>
            <td>Extra</td>
            <td>추가 처리 힌트</td>
            <td>Using where, Using temporary, Using filesort, Using index 등</td>
        </tr>
    </tbody>
</table>

<br>

### 2.2 `type`을 이해하는 방법

<br>

<table>
    <thead>
        <th>유형</th>
        <th>의미</th>
        <th>대표 상황</th>
    </thead>
    <tbody>
        <tr>
            <td>ALL</td>
            <td>테이블 전체 스캔</td>
            <td>적절한 인덱스가 없거나 전체 조회가 유리한 경우</td>
        </tr>
        <tr>
            <td>index</td>
            <td>인덱스 전체 스캔</td>
            <td>인덱스 순서대로 끝까지 읽는 경우</td>
        </tr>
        <tr>
            <td>range</td>
            <td>인덱스 범위 접근</td>
            <td>>, <, BETWEEN 같은 범위 조건</td>
        </tr>
        <tr>
            <td>ref</td>
            <td>비고유 인덱스 값으로 검색</td>
            <td>category_id = 100</td>
        </tr>
        <tr>
            <td>eq_ref</td>
            <td>JOIN 시 앞선 테이블의 각 행에 대해 PK/UNIQUE를 이용해 최대 한 행 조회</td>
            <td>orders와 users를 PK로 조인하는 상황</td>
        </tr>
        <tr>
            <td>const</td>
            <td>PK/UNIQUE 상수 조건으로 쿼리 전체에서 최대 한 행 조회</td>
            <td>WHERE id = 123</td>
        </tr>
        <tr>
            <td>system</td>
            <td>테이블에 행이 하나뿐인 특별한 경우</td>
            <td>아주 드문 단일 행 테이블</td>
        </tr>
    </tbody>
</table>

<br>

**`eq_ref`와 `const` 차이** : 둘 다 고유 인덱스를 이용해 매우 적은 행을 찾을 수 있지만, `const`는 쿼리 전체에서 상수 조건으로 한 행이 결정되는 접근이며, `eq_ref`는 보통 JOIN 과정에서 앞선 테이블의 **각 행마다** 최대 한 행을 찾는 접근이다. 단순히 둘 다 "고유값 검색"이라고만 외우면 구분이 어렵다.

`type`만으로 무조건 성능을 판단하면 안 된다. 데이터가 매우 작거나 전체 집계가 필요한 경우 `ALL`이 합리적일 수 있으며, `index`도 전체 인덱스를 훑으면 적지 않은 비용이 발생한다.

<br>

### 2.3 `Extra` 해석

- `Using Where` : 행에 조건을 적용해 필터링한다는 의미이다.
- `Using temporary` : 중간 결과를 위한 임시 테이블을 활용한다. 그룹화, 정렬 등에서 나타날 수 있다.
- `Using filesort` : 인덱스 순서만으로 처리하지 못해 별도의 정렬 알고리즘을 사용한다. **반드시 디스크 정렬이라는 뜻은 아니다.**
- `Using index` : 필요한 컬럼을 인덱스에서 읽을 수 있는 **커버링 인덱스** 접근을 나타낼 수 있다.

<br>
<hr>

## 3. 인덱스 사용을 막는 조건 개선

<br>

### 3.1 컬럼에 함수를 적용한 날짜 검색

```sql
-- 개선 전: 날짜 컬럼에 함수 적용
EXPLAIN
SELECT id, name, created_at
FROM products
WHERE YEAR(created_at) = 2025;

-- 범위 인덱스 추가
CREATE INDEX idx_products_created_at ON products (created_at);

-- 개선 후: 인덱스의 범위를 직접 지정
EXPLAIN
SELECT id, name, created_at
FROM products
WHERE created_at >= '2025-01-01' AND created_at < '2026-01-01';
```

일반적인 `created_at` B-Tree 인덱스를 기준으로, `YEAR(created_at)`처럼 컬럼에 함수를 적용하면 해당 인덱스를 이용한 범위 탐색이 어려울 수 있다. 반면 컬럼 자체를 범위 비교하면 `range` 접근이 가능해진다.

다만 **위 실습 데이터는 전부 2025년에 속하므로** 범위 조건이 거의 전체 행을 포함한다. 따라서 인덱스를 만들었어도 옵티마이저가 `ALL`을 선택할 수 있다. 이것은 실패가 아니라 선택도의 영향이다.

``` SQL
-- 좀 더 좁은 조건으로 다시 확인
EXPLAIN
SELECT id, name, created_at
FROM products
WHERE created_at >= '2025-01-01 00:00:00'
  AND created_at <  '2025-01-01 01:00:00'
```

`BETWEEN '2025-01-01' AND '2025-12-31'`은 **2025-12-31 자정까지만** 포함하므로 날짜/시간 컬럼의 1년 조회에는 위처럼 다음 해 시작을 배타적(`<`) 상한으로 쓰는 편이 안전하다.

<br>
<hr>

## 4. 집계 쿼리와 인덱스

### 4.1 날짜 범위의 `COUNT(*)`

```sql
EXPLAIN
SELECT COUNT(*) AS total
FROM products
WHERE created_at >= '2025-01-01 00:00:00'
  AND created_at < '2025-01-01 01:00:00';
```

이미 `idx_products_created_at` 인덱스를 생성했다면 범위 접근이 가능한지 확인한다.

정확한 개수를 세어야 하므로 조건을 만족하는 인덱스 엔트리를 실제로 확인해야 하며, 인덱스가 있다고 `COUNT(*)`가 항상 상수 시간에 끝나는 것은 아니다.

<br>

### 4.2 `GROUP BY` 최적화

```sql
EXPLAIN
SELECT category_id, COUNT(*) AS product_count
FROM products
GROUP BY category_id;

CREATE INDEX idx_products_category_id ON products (category_id);

-- 인덱스 추가 후 확인
EXPLAIN
SELECT category_id, COUNT(*) AS product_count
FROM products
GROUP BY category_id;
```

인덱스가 없는 경우 테이블 스캔과 중간 그룹화 작업이 필요할 수 있다. 

인덱스가 있다면 카테고리별로 정렬된 엔트리를 순서대로 읽는 계획(`type = index`, 경우에 따라 `Using index`)을 선택할 수 있다. 하지만 `GROUP BY`로 모든 행을 세는 작업 자체가 없어지지는 않는다.

<br>
<hr>

## 5. 페이징: OFFSET과 LIMIT

<br>

페이징은 전체 결과를 일정한 크기로 분리해서 조회하는 방식이다. `LIMIT`은 반환 건수, `OFFSET`은 정렬된 결과에서 건너뛸 건수를 지정한다.

```sql
SELECT id, name, price, created_at
FROM products
ORDER BY id DESC
LIMIT 10 OFFSET 20;
```

`ORDER BY id DESC`를 사용했으므로 21번째부터 30번째까지 조회한다. 

**정렬 기준이 없으면 페이지 간 일관된 순서를 보장하기 어렵다.**

<br>

## 5.1 OFFESET이 뒤로 갈수록 느려지는 이유

DB는 보통 정렬된 결과에서 `OFFSET + LIMIT`개의 인덱스 엔트리나 행을 처리해야 한다. 

예를 들어 `OFFSET 50000 LIMIT 10`은 앞의 50,000건을 건너뛰기 위해 많은 작업을 수행한다. 반환행은 10건이어도 읽는 양이 늘어날 수 있다.

```sql
EXPLAIN ANALYZE
SELECT id, name, price, created_at
FROM products ORDER BY id DESC LIMIT 10 OFFSET 0;

EXPLAIN ANALYZE
SELECT id, name, price, created_at
FROM products ORDER BY id DESC LIMIT 10 OFFSET 1000;

EXPLAIN ANALYZE
SELECT id, name, price, created_at
FROM products ORDER BY id DESC LIMIT 10 OFFSET 10000;

EXPLAIN ANALYZE
SELECT id, name, price, created_at
FROM products ORDER BY id DESC LIMIT 10 OFFSET 50000;

EXPLAIN ANALYZE
SELECT id, name, price, created_at
FROM products ORDER BY id DESC LIMIT 10 OFFSET 99990;
```

`EXPLAIN ANALYZE` 출력에는 `actual time=시작...종료`, `rows`, `loops` 등이 표시된다.

<br>
<hr>

## 6. Keyset Pagination (커서 기반 페이징)

<br>

**Keyset Pagination**은 숫자로 몇 개를 건너뛸지 지정하지 않고, 직전 페이지의 마지막 **정렬 키**부터 다음 범위를 조회한다.

<br>

### 6.1 기본 키 기반 페이징 

```sql
-- 1페이지
SELECT id, name, price, created_at
FROM products
ORDER BY id DESC
LIMIT 10;

-- 1페이지 마지막 행 id를 실제 조회결과에서 확인해 대입
-- 새 테이블에 정확히 100,000개를 넣었다면 첫 페이지 마지막 id = 99991
SELECT id, name, price, created_at
FROM products
WHERE id < 99991
ORDER BY id DESC
LIMIT 10;
```

핵심은 `id < 마지막_id`이다. 

PK 인덱스에서 바로 다음 범위를 찾아갈 수 있어 뒤쪽까지 같은 수의 행을 읽는 데 유리하다. 다만 임의의 500페이지로 곧장 점프하기 어렵고, 이전 페이지의 커서를 유지해야 한다는 제약이 있다.

<br>

### 6.2 동일 위치의 OFFSET과 Keyset 비교

비교할 때는 **반드시 같은 정렬 기준과 같은 결과 행**으로 맞춘다.

```sql
-- OFFSET: 50,001번째부터 10개
EXPLAIN ANALYZE
SELECT id, name, price, created_at
FROM products
ORDER BY id DESC
LIMIT 10 OFFSET 50000;

-- Keyset: 새로 생성한 1~100,000의 연속 ID를 가정했을 때 같은 10개
EXPLAIN ANALYZE
SELECT id, name, price, created_at
FROM products
WHERE id < 50001 
ORDER BY id DESC
LIMIT 10;
```

위 비교는 **ID가 1~100,000으로 연속되어 있는 이번 초기 데이터**를 전제로 한 예시이다.

실무에서는 ID에 결번이 생길 수 있으므로 `50001` 같은 산술값을 페이지 위치에서 계산하지 않는다. 실제 이전 페이지의 마지막 ID를 커서로 사용한다.

<table>
    <thead>
        <th>방식</th>
        <th>반환하는 ID (초기 데이터 기준)</th>
        <th>실행 시간 (ms, 직접 기록)</th>
    </thead>
    <tbody>
        <tr>
            <td>OFFSET 50000</td>
            <td>50000 ~ 49991</td>
            <td>16 ms</td>
        </tr>
        <tr>
            <td>Keyset (id < 50001)</td>
            <td>50000 ~ 49991</td>
            <td>0 ms</td>
        </tr>
    </tbody>
</table>

<br>

### 6.3 정렬 키에 중복이 있는 경우

`created_at`만으로 정렬하면 같은 시간에 생성된 여러 행이 존재할 수 있다. 

마지막 시간이 `2025-01-01 10:00:00`이었다고 가정하고 `created_at < ...`만 사용하면 같은 시간의 나머지 행이 빠질 수 있다.

```sql
-- 정렬용 복합 인덱스 추가
CREATE INDEX idx_products_created_at_id
ON products (created_at, id);

-- 첫 페이지: 고유한 정렬 순서를 보장
SELECT id, name, created_at
FROM products
ORDER BY created_at DESC, id DESC
LIMIT 10;

-- 다음 페이지: 위 결과의 마지막 created_at과 id를 사용한다.
SELECT id, name, created_at
FROM products
WHERE created_at < '2025-01-01 23:59:00'
	OR (created_at = '2025-01-01 23:59:00' AND id < 86399)
ORDER BY created_at DESC, id DESC
LIMIT 10;
```

같은 뜻을 더 짧게 쓰면 다음과 같다. 

MySQL에서는 **행 값 비교((a, b) < (?, ?))와 OR로 푼 조건식의 인덱스 범위 최적화가 다르게 나타날 수 있으므로** 두 방식 모두 `EXPLAIN`으로 확인해보는 것이 좋다.

```sql
-- 앞선 페이지의 마지막 키가 (:last_created_at, :last_id)일 때의 형태
-- :이름 파라미터는 Java/JDBC/ORM 등의 바인딩 예시이며 MySQL 콘솔에 그대로 넣을 수 없다.
SELECT id, name, created_at
FROM products
WHERE (created_at, id) < (:last_created_at, :last_id)
ORDER BY created_at DESC, id DESC
LIMIT 10;
```

정렬 기준이 여러 개라면 **정렬 순서, 비교 방향, 복합 인덱스 순서**를 함께 설계해야 한다.

위 예제는 모두 `DESC`로 정렬하므로 `<`를 사용한다.

<br>
<hr>

## 7. 필터와 정렬을 함께 사용하는 복합 인덱스

<br>

```sql
CREATE INDEX idx_products_category_created_id
ON products (category_id, created_at, id);

EXPLAIN
SELECT id, name, price, created_at
FROM products
WHERE category_id = 100
ORDER BY created_at DESC, id DESC
LIMIT 10
```

복합 인덱스는 왼쪽 컬럼부터 차례로 사용된다는 점이 중요하다.

위 인덱스는 `category_id = 100`으로 범위를 좁힌 다음 `created_at, id` 순서로 탐색하는 데 유리하다. `price`, `name`처럼 인덱스에 없는 컬럼을 반환해야 하므로 테이블 룩업(랜덤I/O)이 추가로 발생할 수 있다. 또한 **인덱스를 만들었다고 언제나 옵티마이저가 사용한다는 보장은 없다.**

커서 기반으로 연결하면 다음과 같다.

```sql
-- 예시는 직전 페이지 마지막 키가 (2025-01-01 10:00:00, 50000)일 때
SELECT id, name, price, created_at
FROM products
WHERE category_id = 100
  AND (
      created_at < '2025-01-01 10:00:00'
      OR (created_at = '2025-01-01 10:00:00' AND id < 50000)
  )
ORDER BY created_at DESC, id DESC
LIMIT 10;
```

<br>
<hr>

## 8. 정리

<br>

`EXPLAIN`은 쿼리 최적화의 시작점이다. 조회 조건, 정렬 방식, 인덱스 구조를 함께 살펴봐야 병목을 판단할 수 있다. 일반적인 OFFSET 페이징은 구현이 간단하지만 뒤쪽으로 갈수록 읽고 버리는 양이 늘어난다.

반면 Keyset Pagination은 마지막 정렬 키를 기준으로 필요한 범위만 탐색하므로 대량 데이터에 유리하다. 단, **정렬의 유일성 확보 및 원하는 페이지로의 접근 방식**도 설계해야 한다.

