<br>

# Week 09 - Day 04. 분산 데이터와 Kafka 이벤트 설계

<br>

이번 수업은 앞부분에서 Day03의 남은 내용(실패 처리, 멱등키, Event Sourcing)을 마무리하고, 본격적으로 **MSA에서 지금 무슨 일이 일어나고 있는지 어떻게 볼 것인가** 를 다뤘다.

**요청 하나를 따라가 본다. (분산 추적, Zipkin) → 시스템 전체 상태를 숫자로 본다. (메트릭, Prometheus + Grafana) → 수집 경로를 표준화한다. (OpenTelemetry Collector)**

<br>
<hr>

## 1. Day 03 마무리 : 실패와 중복 다루기

<br>

### 1.1 실패한 메시지 처리 : 재시도와 DLT

컨슈머가 어떤 메시지를 계속 처리하지 못하는데 무한히 재시도 하면, 그 메시지 하나가 파티션을 막아서 뒤에 있는 정상 메시지까지 멈춘다. 이런 메시지를 **Poison Pill** 이라고 한다.

그래서 예외를 두 종류로 나눈다.

<table>
    <thead>
        <th>구분</th>
        <th>예시</th>
        <th>처리</th>
    </thead>
    <tbody>
        <tr>
            <td>재시도할 예외</td>
            <td>일시적인 네트워크 오류, DB 타임아웃</td>
            <td>몇 번 간격을 두고 다시 시도한다. 시간이 지나면 성공할 수 있다.</td>
        </tr>
        <tr>
            <td>재시도하지 않을 예외</td>
            <td>JSON 형식 오류, 지원하지 않는 이벤트 타입</td>
            <td>몇 번을 해도 똑같이 실패한다. 바로 DLT로 보낸다.</td>
        </tr>
    </tbody>
</table>

**DLT(Dead Letter Topic)** 는 처리에 실패한 메시지를 따로 모아 두는 토픽이다. 원본 토픽, 파티션, 오프셋, 예외 내용이 헤더에 함께 남는다. 원인을 고친 뒤 원본 토픽으로 다시 보내면 되는데, 컨슈머가 멱등하므로 다시 처리해도 안전하다.

<br>

## 1.2 API 멱등키 설계

**멱등성 : 같은 요청을 여러 번 보내도 결과가 한 번 보낸 것과 같은 성질**

결제 버튼을 두 번 누르거나 네트워크가 끊겨 클라이언트가 재시도하면, 같은 주문이 두 번 생길 수 있다. 이를 막으려면 서버가 "이 요청을 저번에 본 적 있나?"를 판단할 수 있어야 한다. 그 판단 기준이 **멱등키**다.

설계 결정 중 기억할 것만 추렸다.

- **키는 클라이언트가 만든다.** 재시도 할 때 같은 키를 다시 보낼 수 있는 쪽은 클라이언트뿐이다. 보통 `Idempotency-Key` 헤더에 UUID를 담는다.
- **키는 주문과 같은 DB에 UNIQUE 제약으로 저장한다.** 주문 저장과 한 트랜잭션으로 묶을 수 있기 때문이다. (Redis는 별도 시스템이라 원자적으로 묶기 어렵다. Day 03의 Dual Write와 같은 이유다.)
- **같은 키로 다시 오면 응답 유형이 나뉜다.**

<table>
    <thead>
        <th>상황</th>
        <th>응답</th>
    </thead>
    <tbody>
        <tr>
            <td>같은 키 + 같은 내용</td>
            <td>새로 만들지 않고 저장해 둔 결과를 돌려준다. (200)</td>
        </tr>
        <tr>
            <td>같은 키 + 다른 내용</td>
            <td>클라이언트가 키를 잘못 재사용한 것으로 거절한다. (422)</td>
        </tr>
        <tr>
            <td>같은 키가 동시에 도착</td>
            <td>UNIQUE 제약 때문에 하나만 저장된다. 나머지는 먼저 성공한 결과를 받는다.</td>
        </tr>
    </tbody>
</table>

- **트랜잭션이 롤백되면 키 기록도 롤백된다.** 그래서 실패한 요청은 같은 키로 다시 시도할 수 있다.

**컨슈머 멱등성**도 원리가 같다. API에서 멱등키 역할을 Kafka 이벤트에서는 `eventId`가 한다. 처리한 `eventId`를 비즈니스 데이터 변경과 같은 트랜잭션에 기록해 두고, 이미 있으면 건너뛴다.

<br>

### 1.3 멱등키 vs 분산 락

둘 다 "중복"을 막는 것처럼 보여서 헷갈리지만, 막는 대상이 다르다.

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
            <td>100명이 남은 재고 1개를 동시에 주문</td>
        </tr>
        <tr>
            <td>두 번째 요청은</td>
            <td>첫 번째 결과를 그대로 받는다.</td>
            <td>기다리거나 실패한다.</td>
        </tr>
    </tbody>
</table>

<br>

### 1.4 Event Sourcing

보통은 **현재 상태**만 저장하고 바뀔 때마다 덮어쓴다. Event Sourcing은 반대로 **바뀐 사실(이벤트)을 순서대로 쌓기만 하고,** 현재 상태는 그 이벤트들을 처음부터 다시 적용해서 계산한다.

통장으로 생각하면 쉽다. 잔액 칸 하나만 고쳐 쓰는 것이 기존 방식이고, 입금과 출금 내역을 한 줄씩 쌓아 두고 합쳐서 잔액을 구하는 것이 Event Sourcing이다.

- **장점 :** 모든 변경 이력이 남는다(감사 추적). 과거 특정 시점의 상태를 다시 만들 수 있다. 쌓인 이벤트를 그대로 다른 서비스에 발행하기 좋다.
- **단점 :** 현재 상태를 조회하려면 이벤트를 다시 계산해야 해서 보통 조회용 모델(CQRS)이나 스냅샷이 따로 필요하다. 설계와 운영이 복잡하다.

<br>
<hr>

## 2. 관측성(Observability)의 큰 그림

<br>

MSA에서는 요청 하나가 여러 서비스를 가지고, 서비스마다 컨테이너가 여러 대다. 서버에 접속해서 로그 파일을 하나씩 열어 보는 방식으로는 문제를 찾을 수 없다. 그래서 시스템 밖에서도 내부 상태를 알 수 있게 **신호(Telemetry)** 를 내보내도록 만든다. 대표적인 신호가 다음과 같다.

<table>
    <thead>
        <th>신호</th>
        <th>답하는 질문</th>
        <th>오늘 쓴 도구</th>
    </thead>
    <tbody>
        <tr>
            <td>트레이스 (Trace)</td>
            <td>요청 하나가 어느 서비스의 어디서 시간을 썼나?</td>
            <td>Micrometer Tracing → Zipkin</td>
        </tr>
        <tr>
            <td>메트릭 (Metric)</td>
            <td>시스템 전체가 지금 얼마나 건강한가? (숫자)</td>
            <td>Micrometer → Prometheus → Grafana</td>
        </tr>
        <tr>
            <td>로그 (Log)</td>
            <td>그 순간 무슨 일이 있었나? (문장)</td>
            <td>(-)</td>
        </tr>
    </tbody>
</table>

보통 이렇게 함께 쓴다. **메트릭으로 이상을 발견하고 → 트레이스로 어느 구간이 문제인지 좁히고 → 로그로 원인을 확인한다.** Spring Boot는 Micrometer Tracing을 쓰면 로그에 traceId와 spanId를 자동으로 넣어 주기 때문에, 트레이스에서 찾은 traceId로 로그를 검색할 수 있다.

<br>
<hr>

## 3. 분산 추적 (Distributed Tracing)

<br>

### 3.1 Trace Span

<table>
    <thead>
        <th>용어</th>
        <th>뜻</th>
        <th>비유 (택배)</th>
    </thead>
    <tbody>
        <tr>
            <td>Trace</td>
            <td>최초 요청부터 마지막 처리까지의 전체 경로. 고유한 Trace ID 하나를 가진다.</td>
            <td>운송장 한 장</td>
        </tr>
        <tr>
            <td>Span</td>
            <td>그 안의 작업 한 단위 (HTTP 호출 1번, Kafka 발행 1번 등). 시작/종료 시간, 부모 Span ID, 태그, 에러 여부를 기록한다.</td>
            <td>거쳐 간 물류센터마다 찍히는 입고/출고 기록 한 줄</td>
        </tr>
    </tbody>
</table>

Span끼리는 **부모-자식 관계**로 이어져 트리를 만든다. 가장 위의 Span을 **루트 Span**이라고 한다.

```aiignore
Trace (주문 요청 1건)
└─ 주문 API: POST /api/orders              ← 루트 Span
   ├─ 주문 API → 상품 API 호출
   │  └─ 상품 API: GET /api/products/{id}  ← 다른 서비스지만 같은 Trace
   └─ 주문 이벤트 Kafka 발행
      └─ 주문 이벤트 Kafka 소비            ← 비동기여도 같은 Trace
```

<br>

### 3.2 전파 (Propagation)

서비스가 달라져도 Trace로 이어지는 이유는, **다음 서비스로 요청을 보낼 때 Trace ID를 헤더에 실어 보내기** 때문이다. HTTP면 HTTP 헤더에, Kafka면 메시지 헤더에 담긴다.

W3C 표준 헤더 `traceparent`는 `-`로 구분된 네 부분으로 이루어진다.

<table>
    <thead>
        <th>순서</th>
        <th>내용</th>
        <th>길이</th>
    </thead>
    <tbody>
        <tr>
            <td>1</td>
            <td>버전</td>
            <td>2자리 (00)</td>
        </tr>
        <tr>
            <td>2</td>
            <td>trace-id</td>
            <td>32자리 16진수</td>
        </tr>
        <tr>
            <td>3</td>
            <td>부모 span-id</td>
            <td>16자리 16진수</td>
        </tr>
        <tr>
            <td>4</td>
            <td>플래그 (01이면 샘플링되어 수집 대상)</td>
            <td>2자리</td>
        </tr>
    </tbody>
</table>

<br>

### 3.3 Spring Boot : Sleuth → Micrometer Tracing

Spring Boot 2 시절에는 Spring Cloud Sleuth를 썼지만, Boot 3부터는 **Micrometer Tracing** 이 같은 역할을 한다. 하는 일은 같고 의존성과 설정 이름만 바뀌었다.

Micrometer Tracing은 **파사드**다. SLF4J가 로그 구현체를 감추듯, 코드는 Micrometer만 보고 실제 추적은 **Brave**(Zipkin 진영)나 **OpenTelemetry** 중 하나가 맡는다. 수업에는 Brave 브리지 + Zipkin 리포터 조합을 썼다.

<br>

### 3.4 설정에서 꼭 기억할 것

<table>
    <thead>
        <th>설정</th>
        <th>기억할 점</th>
    </thead>
    <tbody>
        <tr>
            <td>management.tracing.sampling.probability</td>
            <td>몇 %의 요청을 추적할지. Spring Boot 기본 값은 0.1(10%)이라, 실습에서 Zipkin에 일부 요청만 보이면 이 값부터 확인한다. 실습은 1.0, 운영은 낮게 잡는다.</td>
        </tr>
        <tr>
            <td>spring.kafka.template/listener.observation-enabled</td>
            <td>기본값 false. 켜지 않으면 HTTP 구간까지만 이어지고 Kafka를 지나는 순간 Trace가 끊긴다.</td>
        </tr>
        <tr>
            <td>management.zipkin.tracing.endpoint</td>
            <td>Span을 보낼 Zipkin 주소. Docker 안에서는 localhost가 아니라 서비스 이름(zipkin)을 쓴다.</td>
        </tr>
        <tr>
            <td>management.observations.annotations.enabled</td>
            <td>@Observed, @Timed 같은 애노테이션을 동작시킨다. (AOP 의존성 필요)</td>
        </tr>
    </tbody>
</table>

<br>

## 3.5 자동으로 잡히는 것과 직접 붙이는 것

- **자동 :** 들어오는 HTTP 요청, Boot가 만들어 준 빌더로 생성한 RestClient/RestTemplate의 나가는 요청, (설정을 켠 경우) Kafka 발행과 소비
- **직접 :** 비즈니스적으로 의미 있는 구간에 `@Observed(name = ...)`을 붙이면 이름 있는 Span이 생긴다. Zipkin 타임라인에서 "주문 생성", "이벤트 발행" 같은 구간을 이름으로 구분할 수 있다. `@Observed`는 Span과 함께 **처리 시간 메트릭도** 만든다.
- **기본 제공 안 됨 :** JDBC 쿼리 단위 Span. 필요하면 별도 라이브러리를 추가한다.

<br>
<hr>

## 4. 메트릭 모니터링 : Prometheus + Grafana ⭐⭐⭐

<br>

### 4.0 헷갈렸던 이유와 핵심 한 문장

Actuator, Micrometer, Prometheus, Grafana 가 한꺼번에 나와서 **누가 메트릭을 만들고, 누가 저장하고, 누가 보여주는지** 경계가 흐려졌다. 핵심은 이 문장이다.

> **앱은 숫자를 "진열"만 하고, Prometheus가 주기적으로 "가져가 저장"을 하고, Grafana는 Prometheus에 "물어봐서 그림"만 그린다.**

편의점 비유로 정리하면 이렇다.

<table>
    <thead>
        <th>구성 요소</th>
        <th>하는 일</th>
        <th>비유</th>
    </thead>
    <tbody>
        <tr>
            <td>Micrometer</td>
            <td>앱 안에서 숫자를 측정하고 기록한다. (요청 수, 응답 시간, 힙 메모리 등)</td>
            <td>매장 직원이 판매 수량을 장부에 기록</td>
        </tr>
        <tr>
            <td>Actuator</td>
            <td>그 숫자를 /actuator/prometheus 라는 창구로 내놓는다. (Prometheus가 읽는 텍스트 형식)</td>
            <td>장부를 계산대 옆에 펼쳐 둠</td>
        </tr>
        <tr>
            <td>Prometheus</td>
            <td>정해진 주기마다 각 앱의 창구에 가서 현재 값을 가져가 시간과 함께 저장한다. (시계열 DB) 알림 규칙도 평가한다.</td>
            <td>본사 직원이 15초마다 매장을 돌며 장부를 사진 찍어 보관</td>
        </tr>
        <tr>
            <td>Grafana</td>
            <td>데이터를 저장하지 않는다. Prometheus에 PromQL로 물어보고 결과를 그래프로 그린다.</td>
            <td>본사 회의실의 대형 모니터</td>
        </tr>
    </tbody>
</table>

```aiignore
[order-api]                       [Prometheus]                     [Grafana]
 Micrometer가 측정                  5~15초마다                        PromQL로 질의
   ↓                               GET /actuator/prometheus          ↓
 /actuator/prometheus  ◀────────── (Pull, scrape) ──▶ 시계열 저장 ◀── 그래프, 대시보드
 (현재 값만 보여줌)                                    (과거 값 누적)
```

헷갈리기 쉬운 지점 두 가지를 짚는다.

- **/actuator/prometheus**에는 "지금 값"만 있다. 과거 값은 앱이 기억하지 않는다. 시간에 따른 변화를 볼 수 있는 이유는 Prometheus가 주기적으로 가져가서 쌓아 두기 때문이다.
- **Grafana는 앱에 직접 붙지 않는다.** Grafana의 데이터소스는 Prometheus다. 앱 → Prometheus → Grafana 순서이고, Grafana가 꺼져 있어도 수집은 계속된다.

<br>

### 4.1 왜 Pull 방식인가?

<table>
    <thead>
        <th>구분</th>
        <th>Pull (Prometheus)</th>
        <th>Push (앱이 직접 전송)</th>
    </thead>
    <tbody>
        <tr>
            <td>앱이 할 일</td>
            <td>창구만 열어 두면 된다.</td>
            <td>수집 서버 주소를 알고 주기적으로 보내야 한다.</td>
        </tr>
        <tr>
            <td>앱이 죽으면</td>
            <td>가져가기가 실패한다. → 그 자체가 신호, up값이 0이 된다.</td>
            <td>"안 보내는 것"인지 "죽은 것"인지 구분이 어렵다.</td>
        </tr>
        <tr>
            <td>수집 주기 변경</td>
            <td>Prometheus 설정 한 곳</td>
            <td>앱마다</td>
        </tr>
    </tbody>
</table>

Prometheus는 수집 대상마다 `up`이라는 값을 자동으로 기록한다. 가져오기에 성공하면 1, 실패하면 0이다. 그래서 `up == 0`은 "이 서비스가 응답하지 않는다"는 가장 기본적인 알림 조건이 된다.

<br>

### 4.2 메트릭의 네 가지 타입

Prometheus 공식 문서는 메트릭 타입을 네 가지로 나눈다. 어떤 타입이냐에 **쓸 수 있는 PromQL이 달라지기 때문에** 꼭 구분해야 한다.

<table>
    <thead>
        <th>타입</th>
        <th>값의 성질</th>
        <th>예시</th>
        <th>Micrometer에서</th>
    </thead>
    <tbody>
        <tr>
            <td>Counter</td>
            <td>늘어나기만 한다. (재시작하면 0부터)</td>
            <td>총 요청 수, 총 주문 수</td>
            <td>Counter</td>
        </tr>
        <tr>
            <td>Gauge</td>
            <td>오르내린다. "지금 몇인가"</td>
            <td>힙 메모리 사용량, 활성 커넥션 수</td>
            <td>Gauge</td>
        </tr>
        <tr>
            <td>Histogram</td>
            <td>값을 구간(버킷)별로 세어 분포를 기록한다.</td>
            <td>응답 시간 분포</td>
            <td>Timer, DistributionSummary (히스토그램 켠 경우)</td>
        </tr>
        <tr>
            <td>Summary</td>
            <td>분포를 앱 쪽에서 미리 백분위로 계산해 보낸다.</td>
            <td>응답 시간 p95</td>
            <td>(미리 계산하는 방식)</td>
        </tr>
    </tbody>
</table>

Counter는 누적값이라 그 자체로는 의미가 없다. "총 10만 건"보다 **"지금 초당 몇 건씩 늘고 있나"** 가 궁금하다. 그래서 Counter는 항상 `rate()`와 함께 쓴다.

Histogram과 Summary는 둘 다 백분위를 구할 수 있지만, Summary는 앱 인스턴스마다 따로 계산된 값이라 **여러 인스턴스를 합칠 수 없다.** Histogram은 버킷 개수만 기록하므로 여러 인스턴스의 버킷을 더한 뒤 Prometheus에서 백분위를 계산할 수 있다. MSA처럼 인스턴스가 여러 대면 Histogram이 유리하다. 수업의 `percentiles-histogram : true` 설정이 바로 HTTP 응답 시간을 Histogram 버킷으로 내보내게 하는 설정이다.

<br>

### 4.3 이름이 바뀌어서 헷갈린다 : Micrometer 이름 → Prometheus 이름

코드에서는 `http.server.requests` 라고 썼는데 Grafana 에서는 `http_server_requests_seconds_count`로 조회한다. 이것도 헷갈렸던 지점이다.

Micrometer 공식 문서에 따르면 Micrometer는 **소문자 + 점(.)** 으로 이름을 짓고, 각 모니터링 시스템으로 내보낼 때 그 시스템의 이름 규칙에 맞게 **자동으로 변환** 한다. Prometheus로 갈 때의 규칙은 대략 다음과 같다.

- 점(.)은 밑줄(_)로 바뀐다.
- 시간을 재는 Timer에는 기본 단위 `_seconds`가 붙는다.
- Counter에는 `_total`이 붙는다.
- Timer와 Histogram은 **시계열 여러 개로 쪼개져** 나간다.

<table>
    <thead>
        <th>Micrometer 코드</th>
        <th>Prometheus에서 보이는 이름</th>
        <th>의미</th>
    </thead>
    <tbody>
        <tr>
            <td>Timer http.server.requests</td>
            <td>http_server_requests_seconds_count</td>
            <td>요청 건수 (Counter)</td>
        </tr>
        <tr>
            <td></td>
            <td>http_server_requests_seconds_sum</td>
            <td>응답 시간 합계 (Counter)</td>
        </tr>
        <tr>
            <td></td>
            <td>http_server_requests_seconds_bucket{le="0.1"}</td>
            <td>0.1초 이하였던 요청 수(히스토그램 켠 경우)</td>
        </tr>
        <tr>
            <td>Gauge jvm.memory.used</td>
            <td>jvm_memory_used_bytes</td>
            <td>현재 메모리 사용량</td>
        </tr>
        <tr>
            <td>Counter order.created.count</td>
            <td>_total이 붙은 이름</td>
            <td>누적 주문 수</td>
        </tr>
    </tbody>
</table>

여기에 수업의 `management.metrics.tags.application` 설정 덕분에 모든 메트릭에 `application="order-api"` **라벨**이 붙는다. 라벨은 같은 이름의 메트릭을 서비스, URL, 메서드별로 나눠 보는 **분류 기준**이다.

<br>

### 4.4 PromQL 읽는 법

수업의 패널 쿼리를 안쪽부터 읽으면 쉽다.

#### ① 패널 A : JVM 힙 메모리 (Gauge)

```java
sum by (application) (jvm_memory_used_bytes{area="heap"})
```

1. `jvm_memory_used_bytes{area="heap"}` : 메모리 중 힙 영역만 고른다. 힙도 여러 구역(Eden, Old 등)으로 나뉘어 시계열이 여러 개다.
2. `sum by (application)` : 그것들을 서비스별로 **더한다.**

Gauge는 현재 값 자체가 의미 있으니 그대로 그린다. 계속 올라가기만 하고 GC 후에도 내려오지 않으면 메모리 누수를 의심한다.

#### ② 패널 B : 초당 요청 수 RPS (Counter)

```java
sum by (application, method, uri)
(rate(http_server_requests_seconds_count{uri ~ "/actuator.*"}[1m]))
```

1. `http_server_requests_seconds_count{uri!~"/actuator.*"}` : 요청 건수 중 Actuator 요청(Prometheus 자신의 수집 요청)은 **제외**한다. `!~`는 정규식 불일치다.
2. `[1m]` : 최근 1분 구간의 값들을 가져온다.
3. `rate(...)` : 그 1분 동안 **초당 평균 몇 씩 늘었는지** 계산한다. 누적값이 초당 증가량으로 바뀐다.
4. `sum by (application, method, uri)` : 인스턴스가 여러 대여도 서비스, 메서드, URI 기준으로 합친다.

`rate()`는 **Counter에만** 쓴다. 앱이 재시작해서 Counter가 0으로 돌아가도 `rate()`가 이를 감지해 보정해준다. Gauge에 `rate()`를 쓰면 의미 없는 값이 나온다.

#### ③ 응답 시간 p95 (Histogram)

```aiignore
histogram_quantile(0.95, sum by (le, application)
(rate(http_server_requests_seconds_bucket[1m])))
```

1. 버킷별 초당 증가량을 구하고 (`rate`), 인스턴스를 합친다(`sum`).
2. 이 때 **`le`(버킷 상한) 라벨은 꼭 남겨야** 한다. Prometheus 공식 문서도 `histogram_quantile()`은 `le` 라벨이 필요하므로 `by` 절에 포함하라고 안내한다.
3. `histogram_quantile(0.95, ...)` : 버킷 분포로 "요청 95%가 몇 초 안에 끝났나"를 계산한다.

평균 응답 시간은 느린 요청 몇 개가 묻혀 버리므로, 사용자가 체감하는 지연은 p95, p99로 본다.

<table>
    <thead>
        <th>타입</th>
        <th>그대로 그리기</th>
        <th>함께 쓰는 함수</th>
    </thead>
    <tbody>
        <tr>
            <td>Gauge</td>
            <td>O</td>
            <td>sum, avg, max</td>
        </tr>
        <tr>
            <td>Counter</td>
            <td>X (계속 커지기만 함)</td>
            <td>rate() 후 sum</td>
        </tr>
        <tr>
            <td>Histogram</td>
            <td>X</td>
            <td>rate() → sum by (le) → histogram_quantile()</td>
        </tr>
    </tbody>
</table>

<br>

### 4.5 Grafana에서 자주 막히는 지점 : localhost

Grafana에서 Prometheus 데이터소스를 직접 등록할 때 URL에 `http://localhost:9090`을 넣으면 연결되지 않는다.

Grafana의 기본 접속 방식(Proxy) 에서는 **브라우저가 아니라 Grafana 서버(컨테이너)가** Prometheus에 접속한다. 컨테이너 안에서 `localhost`는 **Grafana 컨테이너 자기 자신**이다. 그래서 같은 Docker 네트워크의 **서비스 이름**을 써서 `http://prometheus:9090`으로 등록해야 한다.

같은 이유로 `prometheus.yml`의 수집 대상도 `localhost:8083`이 아니라 `order-api:8083` 이고, Zipkin 주소도 Docker 안에서는 `zipkin:9411`이다. **"Docker 안에서는 localhost 대신 서비스 이름"** 으로 기억한다. (`prometheus` 잡의 `localhost:9090`은 Prometheus가 자기 자신을 수집하는 것이라 예외다.)

수업에서는 **provisioning** 폴더에 데이터소스와 대시보드 설정 파일을 넣어 두어서, Grafana가 뜰 때 자동으로 등록되게 했다. UI에서 손으로 등록하지 않아도 되고, 설정을 코드로 관리할 수 있다.

<br>

## 4.6 비즈니스 메트릭

JVM이나 HTTP 같은 기술 메트릭은 Spring Boot가 자동으로 만들어 주지만, "주문이 몇 건 생겼나"는 직접 기록해야 한다.

<table>
    <thead>
        <th>측정 대상</th>
        <th>쓴 것</th>
        <th>타입</th>
    </thead>
    <tbody>
        <tr>
            <td>주문 생성 처리 시간</td>
            <td>@Timed</td>
            <td>Timer</td>
        </tr>
        <tr>
            <td>생성된 주문 수</td>
            <td>Counter</td>
            <td>Counter</td>
        </tr>
        <tr>
            <td>주문 1건당 수량 분포</td>
            <td>DistributionSummary</td>
            <td>시간이 아닌 값의 분포</td>
        </tr>
    </tbody>
</table>

주문 수 Counter를 **`@TransactionalEventListener`** 에서 올린 점이 중요하다. 커밋이 끝난 뒤에 실행되므로 **롤백된 주문은 세지 않는다.** 메트릭도 "실제로 확정된 사실"만 세야 정확하다는 점에서 Day03의 Outbox와 같은 고민이다.

<br>

### 4.7 포트 정리

<table>
    <thead>
        <th>포트</th>
        <th>누구</th>
        <th>용도</th>
    </thead>
    <tbody>
        <tr>
            <td>8083</td>
            <td>order-api</td>
            <td>/actuator/prometheus 창구</td>
        </tr>
        <tr>
            <td>9090</td>
            <td>Prometheus</td>
            <td>저장소 + 쿼리 UI</td>
        </tr>
        <tr>
            <td>3000</td>
            <td>Grafana</td>
            <td>대시보드</td>
        </tr>
        <tr>
            <td>9411</td>
            <td>Zipkin</td>
            <td>Span 수집과 조회 UI</td>
        </tr>
        <tr>
            <td>4318</td>
            <td>OTel Collector</td>
            <td>앱이 OTLP(HTTP)로 메트릭을 보내는 곳</td>
        </tr>
        <tr>
            <td>8889</td>
            <td>OTel Collector</td>
            <td>Prometheus가 메트릭을 가져가는 곳</td>
        </tr>
        <tr>
            <td>8888</td>
            <td>OTel Collector</td>
            <td>Collector 자기 자신의 상태 메트릭</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

## 5. OpenTelemetry Collector로 수집 경로 확장하기

<br>

### 5.1 왜 필요한가

지금까지는 Prometheus가 서비스마다 직접 수집했다. 서비스가 적으면 충분하지만 많아지면 문제가 생긴다. 저장소를 바꾸거나 하나 더 추가하려면 **모든 서비스를 수정하고 재배포**해야 하고, 메트릭, 트레이스, 로그가 제각각 다른 경로로 나간다.

**OpenTelemetry(OTel)** 는 메트릭, 트레이스, 로그를 **하나의 표준 데이터 모델과 전송 프로토콜 (OTLP)** 로 다루는 CNCF 프로젝트다. **OTel Collector** 는 앱들과 저장소 사이에서 서는 **중계소**다.

<br>

### 5.2 Collector의 구조

OTel 공식 문서는 Collector를 세 종류의 부품을 이은 **파이프라인**으로 설명한다.

<table>
    <thead>
        <th>부품</th>
        <th>하는 일</th>
        <th>실습에서</th>
    </thead>
    <tbody>
        <tr>
            <td>Receiver</td>
            <td>데이터를 받는다.</td>
            <td>otlp : 앱이 보낸 OTLP를 4318 포트로 받는다.</td>
        </tr>
        <tr>
            <td>Processor</td>
            <td>받은 데이터를 가공한다. (묶어 보내기, 라벨 추가/삭제, 필터링)</td>
            <td>공통 처리를 여기서 한 번에</td>
        </tr>
        <tr>
            <td>Exporter</td>
            <td>원하는 저장소로 내보낸다.</td>
            <td>prometheus : 8889 포트에 Prometheus 형식으로 진열</td>
        </tr>
    </tbody>
</table>

<br> 

### 5.3 Push와 Pull이 섞인 구조

이번 예제의 흐름이 처음엔 헷갈렸는데, **앞쪽은 Push, 뒤쪽은 Pull** 이다.

```aiignore
[order-api] ──Push(OTLP)──▶ [Collector :4318 → :8889] ◀──Pull── [Prometheus] ◀── [Grafana]
```

- 앱은 OTLP 표준과 Collector 주소 하나만 안다. 저장소가 무엇인지 모른다.
- Prometheus는 여전히 Pull로 가져가므로 **PromQL과 대시보드를 그대로** 쓸 수 있다.

<br>

### 5.4 코드는 그대로, Registry만 바뀐다.

`@Timed`, `Counter` 코드는 한 줄도 바뀌지 않았고 **의존성만** `micrometer-registry-prometheus` 에서 `micrometer-registry-otlp`로 바꿨다. SLF4J를 쓰는 코드가 Logback이든 Log4j2든 신경쓰지 않는 것처럼, 메트릭 코드는 Micrometer만 보고 **어디로 내보낼지는 Registry가 정한다.**

<br>

### 5.5 장단점

<table>
    <thead>
        <th>구분</th>
        <th>Prometheus 직접 수집</th>
        <th>Collector 경유</th>
    </thead>
    <tbody>
        <tr>
            <td>저장소 교체, 추가</td>
            <td>모든 서비스 수정</td>
            <td>Collector 설정만 수정</td>
        </tr>
        <tr>
            <td>공통 가공</td>
            <td>서비스마다</td>
            <td>Collector 한 곳</td>
        </tr>
        <tr>
            <td>신호 종류</td>
            <td>신호마다 다른 경로</td>
            <td>메트릭, 트레이스, 로그를 같은 경로로</td>
        </tr>
        <tr>
            <td>단점</td>
            <td>서비스와 저장소가 강하게 묶임</td>
            <td>운영할 구성 요소가 늘고, Collector가 새로운 장애 지점이 됨</td>
        </tr>
    </tbody>
</table>

마지막 단점 때문에 **Collector 자신도 모니터링 대상**이다. Collector가 막히면 모든 서비스의 그래프가 한 번에 끊긴다. 그래서 Collector가 8888 포트에 내놓는 자기 상태 메트릭(받은 양, 내보낸 양, 실패한 양)을 Prometheus가 따로 수집하고, 대시보드에 별도 패널로 둔다.

<br>
<hr>

## 6. 핵심 정리

- 처리할 수 없는 메시지는 재시도할 예외가 아닌 예외로 나누고, 끝내 실패하면 **DLT**로 보내 파티션이 막히지 않게 한다.
- **멱등키**는 같은 요청의 중복 반영을, **분산 락**은 다른 요청의 동시 수정을 막는다. 멱등키는 비즈니스 데이터와 같은 DB, 같은 트랜잭션에 저장한다.
- **Event Sourcing**은 현재 상태 대신 변경 이벤트를 쌓고, 상태는 이벤트를 재생해서 계산한다.
- **트레이스는 요청 하나, 메트릭은 시스템 전체**를 본다. 메트릭으로 이상을 찾고 트레이스로 구간을 좁힌다.
- Trace는 여러 Span의 트리이고, Trace ID가 **헤더로 전파**되어 서비스와 Kafka를 넘어 이어진다. Kafka 구간은 `observation-enabled`를 켜야 이어진다.
- **Micrometer가 측정하고, Actuator가 진열하고, Prometheus가 주기적으로 가져가 저장하고, Grafana가 질의해서 그린다.**
- Counter는 `rate()`, Historygram은 `rate()` + `sum by (le)` + `histogram_quantile()`, Guage는 그대로 본다.
- Docker 안에서는 **localhost 대신 서비스 이름**으로 접속한다.
- **OTel Collector**는 앱과 저장소 사이의 중계소다. 앱은 **OTLP**로 보내기만 하고, 저장소 교체와 공통 가공은 Collector에서 한다. 대신 Collector 자신도 감시해야 한다.

<br>

