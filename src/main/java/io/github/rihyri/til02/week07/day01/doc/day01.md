<br>

# Week 07 - Day 01. MySQL 인덱스 설계 및 활용

<br>

> 학습 주제: B-Tree 인덱스, 인덱스 유형, 복합 인덱스 설계, 실행 계획(EXPLAIN) 분석
> 
> 실습 환경: MySQL 8.0 이상 / InnoDB / 별도 Java 코드 없이 SQL로 실행

<br>
<hr>

## 1. 인덱스(Index)란?

<br>

인덱스는 테이블에서 원하는 데이터를 더 빠르게 찾도록 도와주는 자료구조이다.

책의 목차처럼 검색 조건에 해당하는 위치를 좁혀, 모든 행을 확인하는 작업을 줄여준다.

```sql
SELECT * FROM products WHERE category_id = 500;
```

- **인덱스가 없을 때** : 조건에 맞는 행을 찾기 위해 테이블 전체를 읽을 수 있다.
- **인덱스가 있을 때** : `category_id`를 기준으로 후보 행을 빠르게 찾아갈 수 있다.
- **주의** : 데이터 양이 적거나 조회 결과가 대부분의 행을 차지한다면 인덱스를 사용하지 않는 편이 유리할 수 있다.


<table>
    <thead>
        <th>장점</th>
        <th>단점</th>
    </thead>
    <tbody>
        <tr>
            <td>검색 범위를 줄여 조회 성능 향상</td>
            <td>인덱스 자체의 저장 공간 필요</td>
        </tr>
        <tr>
            <td>범위 조건과 정렬에 활용 가능</td>
            <td>INSERT/UPDATE/DELETE 시 유지 비용 발생</td>
        </tr>
        <tr>
            <td>JOIN 대상 탐색에 활용 가능</td>
            <td>인덱스가 많을수록 관리와 쓰기 비용 증가</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

## 2. B-Tree 인덱스 동작 원리

<br>

### 2.1 B-Tree란?

`B-Tree`는 데이터를 **정렬된 키 순서로 관리**하면서 원하는 값을 효율적으로 검색하기 위한 **균형 탐색 트리**이다. 한 노드가 여러 개의 키와 자식 노드를 가질 수 있으며, 데이터가 늘어나거나 줄어들어도 모든 리프 노드가 동일한 깊이에 있도록 균형을 유지한다.

일반적인 이진 탐색 트리는 노드 하나에서 두 방향으로 분기하지만, B-Tree는 여러 방향으로 분기할 수 있다. 덕분에 많은 데이터를 저장해도 트리의 높이를 비교적 낮게 유지하며, 디스크 페이지를 읽는 횟수를 줄이는 데 유리하다.

- **키(Key)** : 정렬 및 검색의 기준이 되는 값이다. 예: `category_id`, `price`
- **포인터(Pointer)** : 하위 노드 또는 데이터 위치를 찾아가기 위한 참조 정보이다.
- **루트(Root)** : 검색을 시작하는 최상위 노드이다.
- **내부 노드(Internal Node)** : 키의 범위를 비교하여 이동할 하위 노드를 결정한다.
- **리프(Leaf)** : 검색 대상 키와 레코드를 식별할 정보를 담는 마지막 단계이다.

> **B-Tree**와 **B+Tree** 구분 : 
> 
> 수업에서 설명하는 B-Tree는 기본 탐색 원리를 이해하기 위한 개념이다.
> 
> 실제 **MySQL InnoDB 인덱스는 B+Tree 계열**로, 내부 페이지는 탐색 경로를 안내하고 리프 페이지에서 인덱스 항목을 저장한다. 리프 페이지들이 키 순서로 연결되어 있어 범위 검색에 특히 유리하다.

<br>

### 2.2 트리 구조 이해하기

아래는 검색 경로를 이해하기 위해 단순화한 그림이다. 

```aiignore
                         [ 30 | 60 ]           ← 루트
                       /      |      \
                      /       |       \
                [10 | 20] [40 | 50] [70 | 80]  ← 리프(단순화)
                 10  20    40  50    70  80
```

예를 들어 `50`을 찾는다면 다음과 같이 이동한다.
1. 루트에서 `50`을 `30`, `60`과 비교한다.
2. `30 < 50 < 60`이므로 가운데 하위 페이지로 이동한다.
3. 해당 리프에서 `50`을 찾아 검색을 마친다.

여기서 중요한 점은 **모든 데이터를 처음부터 확인하지 않고**, 비교할 때마다 탐색 범위를 줄여 나간다는 것이다.

균형 잡힌 트리의 키 탐색 비용은 일반적으로 데이터 개수에 대해 **로그 수준**으로 증가한다.

다만 실제 SQL 실행 시간은 인덱스 탐색 외에 페이지 읽기, 일치 행 개수, 추가 데이터 접근 등에 영향을 받는다.

<br>

### 2.3 SELECT는 어떻게 동작할까?
#### ① 정확히 일치하는 값 검색 (`=`)
```SQL
-- category_id에 인덱스가 있다고 가정
SELECT id, name, price FROM products
    WHERE category_id = 500
```

- 루트 → 내부 페이지 → 리프 페이지 순서대로 `500`이 시작되는 위치를 찾는다.
- 같은 `category_id`를 가진 인덱스 항목들을 읽는다.
- 조회하려는 컬럼이 인덱스에 없다면, 필요한 행을 기본 키로 다시 읽을 수 있다.

<br>

#### ② 범위 검색 (`BETWEEN`, `>`, `<`)
```sql
-- price에 인덱스가 있다고 가정
SELECT id, name, price FROM products
    WHERE price BETWEEN 100 AND 300;
```

- 먼저 `100` 이상인 첫 지점을 트리 검색으로 찾는다.
- 이후 리프 페이지에 정렬된 인덱스 항목을 순서대로 읽는다.
- `300`을 초과하면 범위 탐색을 종료할 수 있다.

<br>

#### ③ 정렬에 활용 (`ORDER BY`)
```sql
SELECT id, price
    FROM products
ORDER BY price ASC
LIMIT 10;
```

`price` 인덱스를 활용할 수 있다면 이미 정렬된 인덱스 순서대로 앞의 10건을 가져올 수 있어 별도 정렬을 줄일 수 있다. 

다만 쿼리의 조건과 조회 컬럼에 따라 정렬 작업이 추가될 수 있다.

<br>

### 2.4 INSERT, UPDATE, DELETE에서는 어떤 일이 일어날까?

인덱스가 빠른 검색을 제공하는 대신 **데이터 변경 시 정렬 상태와 트리 구조를 유지하는 비용**이 발생한다.

#### · INSERT 예시
```sql
INSERT INTO products (name, description, price, stock, category_id, status, created_at, updated_at)
VALUES ('테스트 키보드', '인덱스 삽입 실습', 150.00, 10, 500, 'ACTIVE', NOW(), NOW());
```

1. 삽입할 값이 들어갈 리프 페이지를 탐색한다.
2. 페이지에 공간이 있으면 정렬된 인덱스 항목을 추가한다.
3. 페이지가 가득 차 있으면 **페이지 분할(Page Split)** 이 발생할 수 있다.
4. 분할 이후 상위 페이지의 경계 키가 조정되며, 경우에 따라 상위 페이지도 분할된다.

```aiignore
[10 | 20 | 30 | 40]  ← 가득 찬 페이지에 25를 삽입한다고 가정
         ↓
[10 | 20 | 25]  →  [30 | 40]  ← 분할 결과를 단순화한 예시
         ↑ 상위 페이지의 경계 키 정보도 갱신
```

- **UPDATE** : 인덱스에 포함된 컬럼의 값이 변경되면 해당 인덱스 항목의 위치를 조정하거나 삭제 후 다시 추가하는 작업이 필요할 수 있다.
- **DELETE** : 인덱스 항목의 삭제 처리가 필요하며, InnoDB는 삭제 표시 후 나중에 정리하는 경우도 있다. 항상 즉시 노드가 합쳐지는 것은 아니다.

따라서 인덱스를 많이 만들수록 조회에 유리한 쿼리가 생길 수 있지만, **쓰기 작업과 저장 공간에는 부담**이 늘어난다.

<br>

### 2.5 InnoDB의 클러스터형 인덱스와 보조 인덱스

MySQL InnoDB에서는 리프 페이지에 무엇이 저장되는지 구분하는 것이 중요하다.

<table>
    <thead>
        <th>구분</th>
        <th>리프 페이지에 저장되는 정보</th>
        <th>조회 특성</th>
    </thead>
    <tbody>
        <tr>
            <td>클러스터형 인덱스(기본 키)</td>
            <td>기본 키 기준으로 정렬된 실제 행 데이터</td>
            <td>기본 키를 따라 필요한 행에 접근</td>
        </tr>
        <tr>
            <td>보조 인덱스 (Secondary Index)</td>
            <td>보조 인덱스 키와 해당 행의 기본 키 값</td>
            <td>필요한 컬럼이 없으면 기본 키 인덱스를 추가 탐색</td>
        </tr>
    </tbody>
</table>

예를 들어 `category_id` 보조 인덱스로 상품을 찾더라도 `name`, `description` 등 필요한 데이터가 그 인덱스가 없으면 기본 키로 행을 다시 조회할 수 있다.

반대로 요청한 컬럼들이 인덱스만으로 충족된다면 추가적인 행 조회를 줄이는 **커버링 인덱스(Covering Index)** 방식이 가능하다.

<br>

### 2.6 핵심 정리
- B-Tree는 **정렬된 키를 관리하는 균형 탐색 트리**로, 트리 높이를 낮게 유지하여 빠르게 검색한다.
- InnoDB에서 주로 사용하는 **B+Tree**는 리프 페이지를 순서대로 읽을 수 있어 **범위 검색**에 유리하다.
- 검색은 **루트 → 내부 페이지 → 리프 페이지** 순서로 탐색 범위를 좁힌다.
- 데이터 변경 시에는 인덱스 유지와 페이지 분할 등의 **추가 비용**이 발생할 수 있다.
- 인덱스가 존재해도 항상 사용되지 않으므로 **`EXPLAIN`으로 실행 계획을 확인**해야 한다.

<br>
<hr>

## 3. 인덱스 종류

<br>

### 3.1 Primary Key

기본 키는 행을 고유하게 식별하며 `NULL`과 중복을 허용하지 않는다. InnoDB는 기본 키를 중심으로 **클러스터형 인덱스**를 구성한다. 

다만 모든 상황에서 기본 키 조회가 무조건 가장 빠르다고 단정할 수는 없다.

```SQL
CREATE TABLE index_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL
) ENGINE = InnoDB;
```

<br>

### 3.2 Unique Index

특정 값의 중복을 방지할 때 사용한다. 

MySQL에서는 UNIQUE 컬럼이 `NULL`을 허용하도록 정의되면 여러 `NULL`이 저장될 수 있다.

```SQL
CREATE UNIQUE INDEX uq_index_users_email ON index_users(email);
```

<br>

### 3.3 일반(단일 컬럼) 인덱스

```sql
CREATE INDEX idx_products_category ON products(category_id);
```

<br>

### 3.4 복합 인덱스 (Composite Index)

둘 이상의 컬럼을 묶는 인덱스이다. **왼쪽 접두사(Leftmost Prefix)** 규칙을 기준으로 설계한다.

```sql
CREATE INDEX idx_products_category_created ON products(category_id, created_at);

-- 첫 컬럼만 사용
SELECT * FROM products WHERE category_id = 500;

-- 첫 컬럼 동등 비교 후 두 번째 컬럼 범위 비교
SELECT * FROM products
    WHERE category_id = 500 AND created_at >= '2024-01-01';
    
-- 선행 컬럼이 없으므로 효율적인 범위 탐색이 제한될 수 있음
SELECT * FROM products WHERE created_at >= '2024-01-01';
```

> 무조건 카디널리티가 높은 컬럼부터 배치하는 것이 정답은 아니다. **실제 WHERE 조건의 동등/범위 비교와 ORDER BY 순서**를 함께 고려해야 한다.
> 
> MySQL 버전과 통계에 따라 선행 컬럼 없이 특수한 최적화가 가능하지만, 기본 설계 기준은 왼쪽 접두사이다.

<br>

### 3.5 외래 키와 인덱스

외래 키(FK)는 **참조 무결성을 정의하는 제약 조건**이며, 인덱스 그 자체는 아니다.

InnoDB에서는 외래 키 검사에 필요한 인덱스가 있어야 하므로 적절한 인덱스가 없다면 자동 생성될 수 있다.

<br>
<hr>

## 4. WHERE, JOIN, ORDER BY에 인덱스 적용하기

<br>

### 4.1 WHERE : 조건에 맞는 행 찾기

```SQL
SELECT id, name, price FROM products
    WHERE category_id = 500;
```

`category_id`가 선택도가 좋은 조건이고 데이터가 충분하다면 인덱스 사용이 유리하다.

반면 값의 종류가 적은 `status`와 같은 컬럼은 단독 인덱스 효율이 낮을 수 있다.

<br>

### 4.2 JOIN : 연결되는 레코드 찾기

```SQL
SELECT p.name AS product_name, c.name AS category_name
FROM products p
JOIN categories c ON c.id = p.category_id
    WHERE c.id = 500;
```

`categories.id`는 기본 키이고, `products.category_id`에는 인덱스를 둘 수 있다. 

연결 조건뿐 아니라 필터 조건, 테이블 크기에 따라 최적 실행 계획은 달라진다.

<br>

### 4.3 ORDER BY : 정렬 비용 줄이기
```SQL
CREATE INDEX idx_products_price ON products(price);

SELECT id, name, price FROM products
ORDER BY price ASC 
LIMIT 20;
```

인덱스 순서로 결과를 읽으면 별도 정렬을 줄일 수 있다.

다만 JOIN, 다른 필터 조건, 조회 대상 컬럼에 따라 `Using filesort`가 표시될 수 있다.

<br>
<hr>

## 5. 카디널리티와 인덱스 설계

<br>

**카디널리티**는 컬럼의 서로 다른 값의 개수이다.

서로 다른 값이 많으면 검색 대상을 좁히기 쉬운 경우가 많지만, 카디널리티만으로 인덱스의 좋고 나쁨을 판단할 수는 없다.

```sql
SELECT
    COUNT(*) AS total_rows,
    COUNT(DISTINCT category_id) AS category_cardinality,
    COUNT(DISTINCT status) AS status_cardinality
FROM products;
```

인덱스를 정할 때 다음을 함께 확인한다.

- 자주 실행되는 SQL의 `WHERE`, `JOIN`, `ORDER BY` 패턴
- 필터 후 남는 행의 수(선택도)
- 동등 조건과 범위 조건의 배치
- 읽기 성능과 쓰기·저장 공간 비용
- 중복되거나 불필요한 인덱스가 있는지 여부

<br>
<hr>

## 6. MySQL 실습 준비

<br>

```SQL
-- 데이터 확인
SELECT COUNT(*) AS product_count FROM products;
SELECT COUNT(*) AS category_count FROM categories;
SELECT * FROM products LIMIT 5;
```

<br>
<hr>

## 7. 인덱스 유무에 따른 조회 비교

<br>

### 7.1 인덱스 생성 전

```sql
EXPLAIN SELECT * FROM products WHERE category_id = 500;
```

인덱스가 없다면 `type = ALL`, `key = NULL`과 같은 전체 스캔 계획이 나올 수 있다.

<br>

### 7.2 인덱스 생성 후

```sql
CREATE INDEX idx_products_category ON products(category_id);

EXPLAIN SELECT * FROM products WHERE category_id = 500;
```

인덱스가 활용되면 일반적으로 `key = idx_products_category`, `type = ref` 등이 표시된다.

테스트 데이터 특성과 버전에 따라 반드시 같은 결과가 나오지는 않는다.

<br>

### 7.3 실제 실행 비교

```sql
ANALYZE SELECT * FROM products WHERE category_id = 500;
```

`EXPLAIN`은 주로 **예상 실행 계획**, `ANALYZE`는 쿼리를 실제 실행해 **실행 시간과 실제 처리 행 수**도 보여준다. 

MySQL 8.0.18 이상에서 사용할 수 있다.

<br>
<hr>

## 8. EXPLAIN 주요 항목

<table>
    <thead>
        <th>컬럼</th>
        <th>의미</th>
        <th>관찰할 점</th>
    </thead>
    <tbody>
        <tr>
            <td>type</td>
            <td>접근 방식</td>
            <td>ALL, index, range, ref, eq_ref 등을 확인</td>
        </tr>
        <tr>
            <td>possible_keys</td>
            <td>후보 인덱스</td>
            <td>사용할 수 있는 인덱스 목록</td>
        </tr>
        <tr>
            <td>key</td>
            <td>실제 선택한 인덱스</td>
            <td>NULL이면 선택한 인덱스 없음</td>
        </tr>
        <tr>
            <td>rows</td>
            <td>검사할 것으로 예상하는 행 수</td>
            <td>실제 처리량과 차이날 수 있음</td>
        </tr>
        <tr>
            <td>Extra</td>
            <td>추가 처리 정보</td>
            <td>Using where, Using filesort, Using temporary 등</td>
        </tr>
    </tbody>
</table>

<br>

- **ALL** : 전체 테이블 스캔
- **index** : 인덱스 전체 스캔 - 인덱스를 사용했다고 반드시 효율적인 것은 아님
- **range** : 인덱스 범위 스캔
- **ref** : 비고유 인덱스를 활용한 동등 조건 접근 등
- **eq_ref** : JOIN에서 이전 테이블의 행마다 기본 키 또는 고유 키로 최대한 행 조회
- **Using where** : 조건 필터링 수행
- **Using filesort** : 별도 정렬 작업 - 디스크 파일을 사용한다는 뜻은 아님
- **Using temporary** : 내부 임시 테이블 활용 가능성 

<br>
<hr>

## 9. 정리

<br>

1. 인덱스는 **읽기 비용을 낮추는 대신 저장 공간과 쓰기 비용**을 사용한다.
2. B-Tree 계열은 단건 및 범위 검색에 적합하며, InnoDB는 기본 키 기반 클러스터형 인덱스를 사용한다.
3. 복합 인덱스는 **컬럼 순서와 실제 쿼리 조건**을 기준으로 설계해야 한다.
4. `WHERE`, `JOIN`, `ORDER BY`에 인덱스가 도움이 되지만 항상 선택되지는 않는다.
5. 최종 판단은 **EXPLAIN과 실제 측정 결과**를 보고 내린다.

<br>

