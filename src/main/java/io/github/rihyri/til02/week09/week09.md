<br>

# Week 9. MSA, DDD, Kafka Event Design & Observability

<br>

9주차에는 모놀리틱을 MSA로 전환할 때 필요한 개념부터 DDD를 통한 서비스 경계 설계, Kafka 기반 이벤트 시스템 설계, 그리고 분산 환경을 관찰하기 위한 추적과 모니터링까지 학습했다.

8주차가 하나의 애플리케이션 안에서 코드의 구조와 테스트, 보안을 통해 품질을 높이는 과정이었다면, 9주차에는 **애플리케이션이 여러 서비스로 나뉘었을 때 데이터의 일관성을 지키고, 서비스 사이의 메시지를 안전하게 주고받으며, 전체 시스템의 상태를 관찰하는 방법**에 집중했다.

전체적인 학습 흐름은 다음과 같다.

```aiignore
MSA 전환과 메시징 → Saga와 데이터 일관성 → DDD로 경계 나누기

→ 분산 데이터와 서비스 디스커버리 → Kafka 이벤트 설계 → 관측성 (Tracing, Metrics)
```

<br>
<hr>

## 1. MSA 전환과 메시징 시스템

<br>

MSA(Microservices Architecture)는 하나의 애플리케이션을 독립적으로 배포할 수 있는 작은 서비스들로 나누는 구조이다.

MSA가 항상 정답은 아니다. 모놀리틱도 같은 애플리케이션을 여러 대 띄워 확장할 수 있고, MSA는 서비스가 나뉘는 순간부터 네트워크 통신과 데이터 일관성이라는 새로운 복잡성이 생긴다.

<table>
    <thead>
        <th>관점</th>
        <th>모놀리틱</th>
        <th>MSA</th>
    </thead>
    <tbody>
        <tr>
            <td>확장성</td>
            <td>애플리케이션 전체를 함께 확장</td>
            <td>부하가 큰 서비스만 골라서 확장</td>
        </tr>
        <tr>
            <td>유지보수성</td>
            <td>규모가 커지면 변경 영향 범위가 넓어짐</td>
            <td>서비스 단위로 독립 배포 가능</td>
        </tr>
        <tr>
            <td>복잡성</td>
            <td>코드 내부가 복잡해짐</td>
            <td>서비스 간 통신, 분산 데이터, 운영이 복잡해짐</td>
        </tr>
    </tbody>
</table>

MSA를 도입하려면 서비스 경계(Bounded Context), 서비스별 데이터 분리, CI/CD 자동화, 관찰 가능성, 서비스 디스커버리와 API 게이트웨이 같이 기반이 먼저 갖춰져야 한다.

전환할 때는 한 번에 갈아엎지 않고, 기존 시스템을 유지하면서 기능을 하나씩 떼어내는 **Strangler Fig 패턴**을 사용한다.

<br>

#### 메시징과 이벤트

서비스끼리 직접 호출하면 받는 쪽이 장애일 때 보내는 쪽도 함께 실패한다. 메시지 브로커를 사이에 두면 받는 쪽이 잠시 멈춰 있어도 메시지가 보관되어, 다시 살아났을 때 처리할 수 있다.

이때 이벤트는 **명령이 아니라 사실**을 알려야 한다. "재고를 줄여줘"가 아니라 "주문이 생성되었다."라고 발생하면, 이 이벤트를 구독하는 서비스가 늘어나도 보내는 쪽 코드를 수정하지 않아도 된다.

<br>

### Apache Kafka 핵심 용어

Kafka는 메시지를 꺼내면 사라지는 큐가 아니라, **뒤에 계속 기록하는 로그(장부)** 에 가깝다. 메시지를 읽어도 보관 기간 동안 지워지지 않기 때문에 여러 컨슈머 그룹이 같은 메시지를 각자 읽을 수 있다.

<table>
    <thead>
        <th>용어</th>
        <th>설명</th>
    </thead>
    <tbody>
        <tr>
            <td>Broker</td>
            <td>메시지를 저장하고 전달하는 Kafka 서버</td>
        </tr>
        <tr>
            <td>Topic</td>
            <td>메시지를 종류별로 구분하는 이름</td>
        </tr>
        <tr>
            <td>Partition</td>
            <td>토픽을 나눈 단위. 순서는 파티션 안에서만 보장된다.</td>
        </tr>
        <tr>
            <td>Offset</td>
            <td>파티션 안에 메시지의 위치 번호</td>
        </tr>
        <tr>
            <td>Key</td>
            <td>같은 키는 같은 파티션으로 간다. (예: 주문 ID → 주문별 순서 보장)</td>
        </tr>
        <tr>
            <td>Consumer Group</td>
            <td>같은 그룹은 파티션을 나눠 읽고, 다른 그룹은 각자 전부 읽는다.</td>
        </tr>
        <tr>
            <td>Rebalancing</td>
            <td>그룹의 컨슈머가 늘거나 줄 때 파티션을 다시 나누는 과정</td>
        </tr>
        <tr>
            <td>Replication</td>
            <td>파티션을 여러 브로커에 복제해 장애에 대비. 리더가 읽기/쓰기를 담당한다.</td>
        </tr>
    </tbody>
</table>

파티션이 3개인데 같은 그룹의 컨슈머를 5개 띄우면, 2개는 할당받을 파티션이 없어서 쉬게 된다.

<br>

### Kafka 주요 설정

<table>
    <thead>
        <th>설정</th>
        <th>의미</th>
    </thead>
    <tbody>
        <tr>
            <td>bootstrap-servers</td>
            <td>처음 접속할 브로커 주소</td>
        </tr>
        <tr>
            <td>acks=all</td>
            <td>모든 동기화된 복제본(ISR)이 기록을 확인한 뒤 성공 응답. 유실 방지</td>
        </tr>
        <tr>
            <td>enable.idempotence=true</td>
            <td>프로듀서가 재전송해도 브로커에 한 번만 기록되도록 보장. 중복 기록 방지</td>
        </tr>
        <tr>
            <td>group-id</td>
            <td>컨슈머 그룹 이름. 보통 서비스 이름으로 지정</td>
        </tr>
        <tr>
            <td>auto-offset-reset</td>
            <td>커밋된 오프셋이 없을 때 어디서부터 읽을지. earliest는 처음부터, latest(기본값)은 최신부터</td>
        </tr>
    </tbody>
</table>


`latest`로 두면 서비스가 처음 뜨기 전에 쌓인 이벤트를 건너뛰며, 주문이 `PENDING` 상태에 계속 머물 수 있다. 그래서 Saga 이벤트를 받는 컨슈머는 `earliest`로 설정한다.

<br>

### Kafka vs RabbitMQ

<table>
    <thead>
        <th>구분</th>
        <th>Kafka</th>
        <th>RabbitMQ</th>
    </thead>
    <tbody>
        <tr>
            <td>비유</td>
            <td>기록을 남겨 두는 장부</td>
            <td>편지를 배달하는 우체국</td>
        </tr>
        <tr>
            <td>메시지 보관</td>
            <td>읽어도 보관 기간 동안 유지</td>
            <td>소비(ack)되면 삭제</td>
        </tr>
        <tr>
            <td>라우팅</td>
            <td>토픽과 파티션 기준</td>
            <td>Exchange가 규칙(direct, fanout, topic 등)에 따라 큐로 전달</td>
        </tr>
        <tr>
            <td>적합한 곳</td>
            <td>대용량 이벤트 스트림, 여러 서비스의 재처리</td>
            <td>작업 분배, 복잡한 라우팅</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

## 2. 분산 데이터 일관성과 Saga 패턴

<br>

모놀리틱에서는 하나의 DB 트랜잭션으로 주문 생성과 재고 차감을 함께 커밋하거나 롤백할 수 있었다. 

하지만 MSA에서는 서비스마다 자신의 DB를 가지므로, 여러 서비스의 변경을 하나의 트랜잭션으로 묶을 수 없다. 그래서 잠시 불일치하더라도 결국에는 일관된 상태가 되는 **최종적 일관성(Eventual Consistency)** 을 목표로 한다.

#### Saga 패턴과 보상 트랜잭션

Saga는 하나의 비즈니스 흐름을 **각 서비스의 로컬 트랜잭션들의 연속**으로 나누어 처리하는 패턴이다.

중간 단계에서 실패하면 롤백 대신, 이미 커밋된 작업을 반대 동작으로 되돌리는 **보상 트랜잭션 (Compensating Transaction)** 을 실행한다.

```aiignore
[정상 흐름]
T1 주문 생성(PENDING) → T2 결제 → T3 재고 차감

[T3에서 실패한 경우]  ※ 이미 커밋된 단계만, 역순으로 보상
T3 재고 차감 실패 → C2 결제 취소 → C1 주문 취소(CANCELLED)
```

보상할 대상은 **"실패 직전까지 무엇이 커밋되었는가"** 로 정해진다. 실패한 단계 자체는 로컬 트랜잭션이 롤백되므로 되돌릴 필요가 없고, 조회만 한 단계는 되돌릴 데이터가 없다.

또한 Saga에서 격리성(Isolation)이 없어서 중간 상태가 외부에 보인다. 주문의 `PENDING` 상태는 "아직 확정되지 않았다"는 표시(Semantic Lock) 역할을 한다.

<br>

### Choreography vs Orchestration

<table>
    <thead>
        <th>구분</th>
        <th>Choreography (안무형)</th>
        <th>Orchestration (지휘형)</th>
    </thead>
    <tbody>
        <tr>
            <td>방식</td>
            <td>각 서비스가 이벤트를 구독하고 다음 이벤트를 발생</td>
            <td>중앙 오케스트레이터가 각 서비스에 명령</td>
        </tr>
        <tr>
            <td>장점</td>
            <td>결합도가 낮고 구조가 단순함</td>
            <td>전체 흐름을 한 곳에서 파악하기 쉬움</td>
        </tr>
        <tr>
            <td>단점</td>
            <td>참여 서비스가 많아지면 흐름 추적이 어려움</td>
            <td>오케스트레이터에 로직이 집중됨</td>
        </tr>
    </tbody>
</table>

참여하는 서비스가 적고 흐름이 단순하면 Choreography, 단계가 많고 분기가 복잡하면 Orchestration이 어울린다.

<br>
<hr>

## 3. DDD로 서비스 경계 나누기

<br>

DDD(Domain-Driven Design)는 비즈니스 도메인을 중심으로 모델과 코드를 설계하는 방법이다.

MSA에서는 **"서비스를 어디서 자를 것인가"** 에 대한 기준을 제공한다.

DDD에서는 개발자와 도메인 전문가 같은 단어를 같은 의미로 사용하는 **유비쿼터스 언어 (Ubiquitous Language)** 를 중요하게 여긴다.

<br>

### 전략적 설계 : 큰 지도 그리기

<table>
    <thead>
        <th>개념</th>
        <th>설명</th>
    </thead>
    <tbody>
        <tr>
            <td>Bounded Context</td>
            <td>하나의 모델이 일관된 의미를 가지는 경계. 같은 "상품"도 전시, 재고, 배송에서 다른 모델이다.</td>
        </tr>
        <tr>
            <td>Context Map</td>
            <td>바운디드 컨텍스트 간의 관계를 표현한 지도</td>
        </tr>
        <tr>
            <td>서브도메인</td>
            <td>핵심(Core), 지원(Supporting), 일반(Generic)으로 나누어 투자할 곳을 정한다.</td>
        </tr>
    </tbody>
</table>

<br>

### 전술적 설계 : 경계 안의 코드 짜기

<table>
    <thead>
        <th>개념</th>
        <th>설명</th>
    </thead>
    <tbody>
        <tr>
            <td>Entity</td>
            <td>식별자로 구분되며 상태가 변하는 객체</td>
        </tr>
        <tr>
            <td>Value Object</td>
            <td>값 자체로 동등성을 판단하는 불변 객체 (예: Money, Address)</td>
        </tr>
        <tr>
            <td>Aggregate</td>
            <td>항상 함께 규칙을 지켜야 하는 객체 묶음. 루트 엔티티를 통해서만 변경한다.</td>
        </tr>
        <tr>
            <td>Repository</td>
            <td>애그리거트 단위로 저장하고 조회하는 인터페이스</td>
        </tr>
        <tr>
            <td>Domain Service</td>
            <td>한 엔티티에 두기 어려운 도메인 규칙 (예: 배송비 정책)</td>
        </tr>
        <tr>
            <td>Application Service</td>
            <td>도메인 객체를 조합해 유스케이스의 흐름을 조율</td>
        </tr>
        <tr>
            <td>Domain Event</td>
            <td>도메인에서 일어난 의미 있는 사실 (예: 주문 생성됨)</td>
        </tr>
    </tbody>
</table>

애그리거트끼리 객체가 아니라 **ID로 참조**한다. 이 규칙을 지키면 나중에 애그리거트를 별도의 서비스로 떼어내기 쉬워진다.

<br>

### DDD 4계층과 의존성 역전

```aiignore
Presentation (Controller, DTO)
    ↓
Application (응용 서비스: 흐름 조율)
    ↓
Domain (엔티티, VO, 도메인 서비스, Repository 인터페이스)
    ↑
Infrastructure (JPA 구현체, Kafka, 외부 API)
```

Repository 인터페이스(포트)는 도메인에 두고, 구현체(어댑터)는 인프라에 둔다. 의존성이 도메인을 향하므로 **저장 기술이 바뀌어도 도메인 코드는 그대로 유지** 된다.

<br>
<hr>

## 4. 분산 데이터와 서비스 디스커버리

<br>

### Database Per Service

각 서비스는 자신의 DB만 소유하고, 다른 서비스의 DB에 직접 접근하지 않는다. 다른 서비스의 데이터가 필요하면 API로 조회하거나, 이벤트로 받아 필요한 값만 복제해 둔다.

서비스 성격에 맞춰 RDB, NoSQL, 검색 엔진처럼 서로 다른 저장소를 선택할 수 있다. (폴리글랏 저장소)

"주문 당시의 상품명을 보여줄지, 현재 상품명을 보여줄지"처럼 데이터를 어떻게 가져올지는 기술이 아니라 **비즈니스 결정**에 따라 달라진다.

<br>

### 서비스 디스커버리 (Spring Cloud Eureka)

MSA에서는 오토스케일링과 재배포로 인스턴스의 주소가 계속 바뀐다. 그래서 **서비스 이름으로 현재 살아 있는 인스턴스 목록**을 관리하는 서비스 레지스트리가 필요하다.

```aiignore
각 서비스 → Eureka에 자신을 등록하고 주기적으로 heartbeat 전송
호출하는 서비스 → Eureka에서 목록을 받아 캐시
              → http://product-service/... 처럼 서비스 이름으로 호출
              → LoadBalancer가 인스턴스 하나를 골라 실제 주소로 변환
```

Eureka는 호출하는 쪽이 직접 인스턴스를 고르는 **클라이언트 사이드 디스커버리** 방식이다. 목록이 캐시되기 때문에 인스턴스가 죽은 직후에는 잘못된 주소로 요청이 갈 수 있으므로, 타임아웃과 재시도, 서킷 브레이커를 함께 설계해야 한다.

<br>
<hr>

## 5. Kafka 이벤트 시스템 설계

<br>

### 이벤트의 봉투 구조

이벤트는 실제 데이터(payload)와 함께, 이벤트를 식별하고 처리하는 데 필요한 메타데이터를 담는다.

```aiignore
{
  "eventId": "9c1e7a52-...",
  "eventType": "ORDER_CANCELLED",
  "eventVersion": 1,
  "aggregateId": "42",
  "occurredAt": "2026-11-12T10:00:05",
  "payload": { "orderId": 42, "reason": "고객 변심" }
}
```

- `eventId` : 이벤트마다 고유한 값. 컨슈머가 중복 여부를 확인하는 멱등 키가 된다.
- `eventType` : 같은 토픽 안에서 이벤트 종류를 구분한다.
- `eventVersion` : payload 구조가 바뀌어도 기존 컨슈머가 깨지지 않도록 버전을 표시한다.
- `aggregateId` : Kafka 메시지 키로 사용하여 같은 주문의 이벤트 순서를 보장한다.

<br>

### 전달 보장 (Delivery Semantics)

메시지가 몇 번 처리되는지는 **"처리"와 "오프셋 커밋" 중 무엇을 먼저 하느냐**로 결정된다.

<table>
    <thead>
        <th>보장 수준</th>
        <th>순서</th>
        <th>장애 시</th>
    </thead>
    <tbody>
        <tr>
            <td>At-Most-Once</td>
            <td>커밋 → 처리</td>
            <td>유실 가능</td>
        </tr>
        <tr>
            <td>At-Least-Once</td>
            <td>처리 → 커밋</td>
            <td>중복 가능 (대부분의 비즈니스 이벤트에 권장)</td>
        </tr>
        <tr>
            <td>Exactly-Once</td>
            <td>Kafka 트랜잭션으로 읽기, 쓰기, 커밋을 묶음</td>
            <td>Kafka → Kafka 처리 구간에서만 보장</td>
        </tr>
    </tbody>
</table>

At-Least-Once를 선택하면 중복은 피할 수 없으므로, 같은 메시지가 두 번 와도 결과가 같도록 **멱등한 컨슈머 (Idempotent Consumer)** 를 만들어야 한다. 또한 auto-commit은 처리 완료가 아닌 시간 기준으로 커밋하므로, 끄고 처리 후에 직접 커밋한다.

<br>

### Dual Write 문제와 Transactional Outbox

`@Transactional`은 DB만 묶는다. DB 저장과 Kafka 발행은 서로 다른 두 시스템에 쓰는 **Dual Write** 이므로, 어느 한쪽만 성공하면 이벤트가 유실되거나 존재하지 않는 주문의 이벤트(유령 이벤트)가 발생할 수 있다.

**Transactional Outbox 패턴**은 보낼 이벤트를 같은 DB의 outbox 테이블에 같은 트랜잭션으로 저장하고, 별도의 릴레이가 이를 읽어 Kafka로 발행하는 방식이다.

```aiignore
[주문 서비스 - 하나의 트랜잭션]
    orders 저장 + outbox 저장 (PENDING)
        ↓    
[릴레이 - 주기적으로 실행]
outbox 조회 → Kafka 발행 → PUBLISHED로 변경
        ↓
[결제 서비스 - 멱등한 컨슈머
    이미 처리한 eventId면 무시
```

릴레이는 outbox 테이블을 주기적으로 조회하는 **Polling Publisher** 방식이나, DB의 변경 로그를 읽는 **CDC(Change Data Capture)** 방식으로 구현한다. Polling Publisher를 여러 인스턴스에서 실행하면 같은 이벤트를 동시에 가져갈 수 있으므로 주의해야 한다.

Outbox는 유실을 막지만 중복은 막지 못한다. 따라서 **"보내는 쪽은 Outbox로 반드시 보내고, 받는 쪽은 멱등성으로 한 번만 반영한다."**

#### · 순서 역전과 실패 처리
- **순서 역전** : 같은 키로 보내 같은 파티션에 넣는 것으로 예방하고, 버전 비교나 상태 전이 검증(`PENDING`에서만 변경 허용)으로 한번 더 방어한다.
- **재시도와 DLT** : 일시적 오류는 간격을 두고 재시도하고, JSON 형식 오류처럼 반복해도 실패하는 메시지는 **DLT(Dead Letter Topic)** 로 보내 파티션이 막히지 않게 한다.

<br>

### 멱등키 vs 분산 락

<table>
    <thead>
        <th>구분</th>
        <th>멱등키</th>
        <th>분산 락</th>
    </thead>
    <tbody>
        <tr>
            <td>막는 것</td>
            <td>같은 요청이 여러 번 반영되는 것</td>
            <td>서로 다른 요청이 같은 자원을 동시에 바꾸는 것</td>
        </tr>
        <tr>
            <td>예시</td>
            <td>한 사용자의 주문 요청 재시도</td>
            <td>여러 사용자가 남은 재고를 1개를 동시에 주문</td>
        </tr>
        <tr>
            <td>두 번째 요청은</td>
            <td>첫 번째 결과를 그대로 받는다.</td>
            <td>기다리거나 실패한다.</td>
        </tr>
    </tbody>
</table>

<br>

### Event Sourcing

현재 상태만 저장하고 덮어쓰는 대신, **변경된 사실(이벤트)을 순서대로 쌓고 이를 다시 적용해 현재 상태를 계산**하는 방식이다. 통장의 잔액만 고쳐 쓰는 것이 아니라 입출금 내역을 쌓아 잔액을 구하는 것과 같다.

모든 변경 이력이 남는다는 장점이 있지만, 조회를 위해 별도의 조회 모델(CQRS)이나 스냅샷이 필요하다.

<br>
<hr>

## 6. 관측성 (Observability)

<br>

MSA에서는 요청 하나가 여러 서비스를 거치기 때문에, 서버마다 로그를 열어보는 방식으로는 문제를 찾기 어렵다. 그래서 시스템 외부에서는 내부 상태를 알 수 있도록 신호(Telemetry)를 수집한다.

<table>
    <thead>
        <th>신호</th>
        <th>답하는 질문</th>
        <th>도구</th>
    </thead>
    <tbody>
        <tr>
            <td>Metric</td>
            <td>시스템이 지금 얼마나 건강한가?</td>
            <td>Micrometer → Prometheus → Grafana</td>
        </tr>
        <tr>
            <td>Trace</td>
            <td>요청이 어느 서비스의 어디서 시간을 썼나?</td>
            <td>Micrometer Tracing → Zipkin</td>
        </tr>
        <tr>
            <td>Log</td>
            <td>그 순간 무슨 일이 있었나?</td>
            <td>traceId로 로그 검색</td>
        </tr>
    </tbody>
</table>

**메트릭으로 이상을 발견하고, 트레이스로 구간을 좁히고, 로그로 원인을 확인한다.**

#### 분산 추적

하나의 요청 전체를 **Trace**, 그 안의 각 작업 구간을 **Span** 이라고 한다. 서비스를 넘어갈 때 HTTP 헤더나 Kafka 메시지 헤더로 Trace ID를 전달(Propagation)하여 하나의 흐름으로 이어 붙인다.

Spring Boot3부터는 Spring Cloud Sleuth 대신 **Micrometer Tracing**을 사용한다. 기본 샘플링 비율은 10%이고, Kafka 구간은 observation 설정을 켜야 Trace가 이어진다는 점에 주의한다.

<br>

### 메트릭 모니터링 : Prometheus와 Grafana

```aiignore
Spring 서비스 (Micrometer + Actuator)
    └── /actuator/prometheus 에 "현재 값" 공개
            ↑ 주기적으로 가져감 (Pull)
Prometheus
    └── 시간별로 저장, PromQL로 조회
            ↑ 질의
Grafana
    └── 그래프로 시각화 (저장하지 않음)
```

Prometheus가 직접 가져가는 Pull 방식이기 때문에, 수집 대상이 응답하지 않으면 그 자체로 장애를 감지할 수 있다.

<table>
    <thead>
        <th>타입</th>
        <th>값의 성질</th>
        <th>예시</th>
    </thead>
    <tbody>
        <tr>
            <td>Counter</td>
            <td>증가만 한다. 보통 rate()와 함께 사용</td>
            <td>총 요청 수, 총 주문 수</td>
        </tr>
        <tr>
            <td>Gauge</td>
            <td>오르락내리는 현재 값</td>
            <td>힙 메모리, 활성 커넥션 수</td>
        </tr>
        <tr>
            <td>Histogram</td>
            <td>버킷별 개수를 기록. 여러 인스턴스를 합산 가능</td>
            <td>응답 시간 분포, p95</td>
        </tr>
        <tr>
            <td>Summary</td>
            <td>앱에서 백분위를 미리 계산. 인스턴스 간 합산 불가</td>
            <td>응답 시간 p95</td>
        </tr>
    </tbody>
</table>

Micrometer 이름은 Prometheus로 내보낼 때 점이 밑줄로 바뀌고 단위와 접미사가 붙는다. 예를 들어 `orders.created` Counter는 `orders_created_total`로, `http.server.requests` Timer는 `http_server_requests_seconds_count`, `_sum`, `_bucket`으로 나뉘어 조회된다.

<br>

### PromQL 예시

```aiignore
# Gauge : 서비스별 힙 메모리 사용량
sum by (application) (jvm_memory_used_bytes {area="heap"})

# Counter : 초당 요청 수 (최근 1분 기준)
sum by (application) (rate(http_server_requests_seconds_count[1m]))

# Histogram : 응답 시간 p95
histogram_quantile(0.95, sum by (le, application)
(rate(http_server_requests_seconds_bucket[1m])))
```

Counter는 누적값이라 그대로 그리면 계속 올라가기만 하므로 `rate()`로 초당 증가량을 구한다. 평균 응답 시간은 느린 요청이 묻히기 때문에 사용자가 체감하는 지연은 p95, p99로 확인한다.

Grafana 데이터 소스에 `localhost:9090`을 입력하면 안 된다. Grafana 컨테이너 안에서 localhost는 Grafana 자신이므로, 같은 Docker 네트워크의 서비스 이름(prometheus:9090)을 사용해야 한다.

<br>

### OpenTelemetry Collector

애플리케이션과 저장소 사이에 Collector를 두면, 애플리케이션 코드는 그대로 두고 의존성(Registry)만 바꿔 수집 경로를 표준화할 수 있다. 다만 Collector 자체가 새로운 장애 지점이 되므로 Collector도 함께 감시해야 한다.

<br>
<hr>

## 7. 9주차 핵심 정리

<br>

이번 주의 주요 학습 내용을 정리하면 다음과 같다.

1. **MSA와 메시징 :** 서비스를 나누면 통신과 데이터의 일관성의 복잡성이 생기고, 이벤트는 명령이 아닌 사실을 알린다.
2. **Saga 패턴 :** 분산 트랜잭션 대신 로컬 트랜잭션의 연속으로 처리하고, 실패 시 커밋된 단계만 역순으로 보장한다.
3. **DDD :** 바운디드 컨텍스트로 서비스 경계를 정하고, 애그리거트끼리는 ID로 참조한다.
4. **분산 데이터 :** 서비스마다 DB를 분리하고, 서비스 디스커버리로 이름 기반 호출을 한다.
5. **Kafka 이벤트 설계 :** Outbox로 발행을 보장하고, 멱등한 컨슈머로 중복을 안전하게 처리한다.
6. **관측성 :** 메트릭, 트레이스, 로그를 함께 활용해 분산 시스템의 상태를 파악한다.

9주차에는 서비스를 나누는 것 자체보다, **나뉜 서비스들이 실패와 중복 속에서도 결국 올바른 상태에 도달하도록 설계하고, 그 과정을 관찰할 수 있게 만드는 방법** 을 학습했다.

특히 Saga, Outbox, 멱등성, 모니터링은 각각 독립된 기술이 아니라 **"네트워크는 언제든 실패하고 메시지는 중복될 수 있다"** 는 전제 아래에서 서로의 빈틈을 메우는 장치라는 점을 이해할 수 있다.

<br>

