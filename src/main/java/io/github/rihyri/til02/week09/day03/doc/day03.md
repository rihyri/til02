<br>

# Week 09 - Day 03. 분산 데이터와 Kafka 이벤트 설게

<br>

이번 수업은 앞부분에서 Day 02의 남은 내용(DDD/TDD/BDD 비교, 서비스 디스커버리)을 마무리하고, 본격적으로 **"서비스마다 DB를 따로 가지면 데이터를 어떻게 맞출 것인가"** 를 다뤘다.

흐름을 한줄로 요약하면 다음과 같다.

```aiignore
DB를 서비스마다 나눈다. → 저장소도 서비스 성격에 맞게 고른다. → 대신 데이터가 잠깐씩 어긋난다. 

→ 실패하면 보상으로 되돌린다. → 이 모든 것을 이벤트로 안전하게 주고 받는 방법을 설계한다.
```

7장(Kafka 이벤트 시스템 설계)이 가장 낯설고 어려웠던 부분이라 자세하게 정리했다. 

<br>
<hr>

## 1. DDD, TDD, BDD는 어떤 관계인가

<br>

세 단어가 비슷하게 생겨서 경쟁하는 방법론처럼 보이지만, 실제로는 **서로 다른 질문에 답하는 도구**다.

<table>
    <thead>
        <th>질문</th>
        <th>답하는 기법</th>
        <th>한 줄 설명</th>
    </thead>
    <tbody>
        <tr>
            <td>무엇을 어떤 구조로 나눌까?</td>
            <td>DDD (Domain-Driven Design)</td>
            <td>비즈니스 도메인을 중심으로 경계(Bounded Context)와 모델(애그리거트, 엔티티)을 설계한다.</td>
        </tr>
        <tr>
            <td>무엇을 만들어야 한다고 합의했나?</td>
            <td>BDD (Behavior-Driven Development)</td>
            <td>기획자, 개발자, QA가 함께 읽을 수 있는 시나리오로 "시스템이 어떻게 행동해야 하는지"를 정한다.</td>
        </tr>
        <tr>
            <td>그것을 어떻게 안전하게 구현할까?</td>
            <td>TDD (Test-Driven Development)</td>
            <td>실패하는 테스트를 먼저 쓰고, 통과시키고, 다듬는 짧은 주기로 코드를 만든다.</td>
        </tr>
    </tbody>
</table>

순서로 보면 **DDD로 뼈대를 세우고 → BDD로 요구사항을 합의하고 → TDD로 한 조각씩 구현한다.**

<br>

### 1.1 각 기법의 핵심 키워드

- **DDD :** 유비쿼터스 언어(팀 전체가 같은 업무 용어를 쓰는 것), 바운디드 컨텍스트, 애그리거트, 이벤트 스토밍 같은 워크숍으로 도메인을 함께 그려 보며 시작한다.
- **TDD : Red → Green → Refactor** 사이클
    - Red : 아직 없는 기능에 대한 테스트를 먼저 작성한다. 당연히 실패한다.
    - Green : 테스트를 통과할 만큼만 최소한으로 구현한다.
    - Refactor : 테스트가 통과하는 상태를 유지하면서 코드를 정리한다.
- **BDD : Given - When - Then** 형식의 시나리오. Cucumber 공식 문서에 따르면 이 시나리오를 적는 언어를 **Gherkin** 이라고 부르며, `Feature`, `Scenario`, `Given`, `When`, `Then` 같은 키워드로 사람이 읽을 수 있는 명세를 만들고 이것이 그대로 자동화된 테스트가 된다.

<table>
    <thead>
        <th>구분</th>
        <th>DDD</th>
        <th>TDD</th>
        <th>BDD</th>
    </thead>
    <tbody>
        <tr>
            <td>시작점</td>
            <td>도메인 모델링</td>
            <td>실패하는 테스트</td>
            <td>사용자 시나리오</td>
        </tr>
        <tr>
            <td>주로 참여하는 사람</td>
            <td>개발자 + 도메인 전문가</td>
            <td>개발자</td>
            <td>모든 이해 관계자</td>
        </tr>
        <tr>
            <td>대표 도구</td>
            <td>이벤트 스토밍, 모델 다이어그램</td>
            <td>JUnit, AssertJ, Mockito</td>
            <td>Cucumber 등</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

## 2. 서비스 디스커버리와 Spring Cloud Eureka

<br>

### 2.1 왜 필요한가

서비스가 하나일 때는 IP와 포트를 설정 파일에 적어 두면 끝이다. 하지만 MSA 운영 환경에서는 주소가 계속 바뀐다.
- 트래픽에 따라 인스턴스가 자동으로 늘었다가 줄었다 한다. (오토스케일링)
- 배포하거나 장애로 재시작할 때마다 컨테이너의 IP가 바뀐다.
- 같은 서비스가 여러 대 떠있으면 그 중 하나를 골라야 한다.

호출하는 쪽이 이 주소들을 전부 외워둘 수 없다. 그래서 **"서비스 이름 → 지금 살아 있는 인스턴스 주소 목록"** 을 관리하는 전화번호부가 필요하다. 이것이 **서비스 레지스트리**이고, Eureka가 그 역할을 한다.

<br>

### 2.2 클라이언트 서비스 vs 서버 사이드 디스커버리

"목록에서 누가 인스턴스를 고르느냐"에 따라 두 가지로 나뉜다.

<table>
    <thead>
        <th>구분</th>
        <th>클라이언트 서비스</th>
        <th>서버 사이드</th>
    </thead>
    <tbody>
        <tr>
            <td>누가 고르나</td>
            <td>호출하는 서비스가 직접</td>
            <td>앞단의 로드밸런서/인프라가</td>
        </tr>
        <tr>
            <td>예시</td>
            <td>Eureka + Spring Cloud LoadBalancer</td>
            <td>Kubernetes Service, AWS ALB</td>
        </tr>
        <tr>
            <td>장점</td>
            <td>분산 전략을 코드에서 세밀하게 조절</td>
            <td>애플리케이션은 디스커버리를 몰라도 됨</td>
        </tr>
        <tr>
            <td>단점</td>
            <td>언어/프레임워크별 클라이언트 라이브러리 필요</td>
            <td>별도 인프라를 운영해야 함</td>
        </tr>
    </tbody>
</table>

Eureka는 **클라이언트 사이드** 방식이다. 주문 서비스가 Eureka에서 목록을 받아와서, 그 중 하나를 **스스로** 골라 직접 호출한다. Eureka는 목록을 알려줄 뿐 요청을 중계하지는 않는다.

<br>

### 2.3 Eureka의 동작 순서

<table>
    <thead>
        <th>순서</th>
        <th>이름</th>
        <th>하는 일</th>
    </thead>
    <tbody>
        <tr>
            <td>1</td>
            <td>Register</td>
            <td>서비스가 뜰 때 자기 이름, IP, 포트를 Eureka에 등록한다.</td>
        </tr>
        <tr>
            <td>2</td>
            <td>Renew</td>
            <td>주기적으로 "나 아직 살아 있다"는 heartbeat를 보낸다.</td>
        </tr>
        <tr>
            <td>3</td>
            <td>Fetch</td>
            <td>호출하는 쪽이 전체 목록을 받아 자기 메모리에 캐시한다.</td>
        </tr>
        <tr>
            <td>4</td>
            <td>호출</td>
            <td>캐시된 목록에서 LoadBalancer가 인스턴스를 하나 고른다. (기본 라운드로빈)</td>
        </tr>
        <tr>
            <td>5</td>
            <td>Cancel</td>
            <td>정상 종료할 때 등록을 해제한다.</td>
        </tr>
        <tr>
            <td>6</td>
            <td>Eviction</td>
            <td>heartbeat가 일정 시간 끊긴 인스턴스를 목록에서 제거한다.</td>
        </tr>
    </tbody>
</table>

Spring Cloud Netflix 공식 문서에 따르면 heartbeat 기본 주기는 **30초**이고, 인스턴스, 서버, 클라이언트 세 곳의 캐시가 모두 같아질 때까지 기다려야 하므로 **새 인스턴스가 실제로 호출되기까지 heartbeat 3번 정도의 시간이 걸릴 수 있다.**

<br>

### 2.4 꼭 기억할 점 : 목록은 늦게 바뀐다.

각 클라이언트가 목록을 **캐시**해서 쓰기 때문에, 인스턴스가 죽은 직후 얼마 동안은 이미 죽은 주소로 요청이 갈 수 있다. 그래서 디스커버리만 믿지 말고 **타임아웃, 재시도, 서킷 브레이커**를 함께 설계해야 한다.

<br>

### 2.5 설정에서 헷갈렸던 것들

<table>
    <thead>
        <th>설정</th>
        <th>의미</th>
    </thead>
    <tbody>
        <tr>
            <td>register-with-eureka: false, fetch-registry: false</td>
            <td>Eureka 서버 자신에게 주는 설정. 서버가 1대뿐(standalone)이면 등록하거나 받아올 다른 서버가 없으므로 끈다. 공식 문서에서도 standalone 모드에서는 존재하지 않는 동료 서버에 계속 접근을 시도하지 않도록 끄라고 안내한다.</td>
        </tr>
        <tr>
            <td>spring.application.name</td>
            <td>Eureka에 등록되는 서비스 이름(서비스 ID). 호출할 때 이 이름을 주소처럼 쓴다.</td>
        </tr>
        <tr>
            <td>prefer-ip-address: true</td>
            <td>호스트명 대신 IP로 등록한다. 컨테이너 환경에서는 호스트명이 컨테이너 ID 같은 값이라 다른 컨테이너가 해석하지 못할 수 있다.</td>
        </tr>
         <tr>
            <td>instance-id</td>
            <td>같은 서비스 인스턴스 여러 개를 구분하는 식별자. IP와 포트를 넣어 두면 대시보드에 하나하나 구분된다.</td>
        </tr>
        <tr>
            <td>lease-renewal-interval-in-seconds</td>
            <td>heartbeat를 보내는 주기</td>
        </tr>
        <tr>
            <td>lease-expiration-duration-in-seconds</td>
            <td>마지막 heartbeat 이후 얼마나 지나면 죽었는지 판단할지</td>
        </tr>
        <tr>
            <td>enable-self-preservation</td>
            <td>네트워크 문제로 heartbeat가 한꺼번에 많이 끊기면 Eureka가 "인스턴스가 죽은 게 아니라 네트워크가 이상한 것"이라고 보고 제거를 멈추는 보호 모드</td>
        </tr>
    </tbody>
</table>

<br>

### 2.6 서비스 이름으로 호출하기

`@LoadBalanced`를 붙인 HTTP 클라이언트는 http://product-api/...처럼 **호스트 자리에 서비스 이름**을 쓸 수 있다. 실제 호출 직전에 LoadBalancer가 Eureka 목록에서 인스턴스 하나를 골라 진짜 IP:포트로 바꿔준다.

실습에서 가장 인상 깊었던 점은 **도메인/애플리케이션 코드는 한 줄도 바뀌지 않았다**는 것이다. "상품 정보를 조회한다"는 포트(인터페이스)는 그대로 두고, 메모리에서 꺼내던 구현체를 HTTP로 상품 서비스를 호출하는 구현체로 **교체**만 했다. 이때 상품 서비스의 응답 모델을 주문 쪽 모델로 번역해서 들여오는데, 이렇게 외부 모델이 내 도메인으로 새어 들어오지 않게 막는 층을 **ACL(Anti-Corruption Layer, 부패 방지 계층)** 이라고 한다.

<br>
<hr>

## 3. Database per Service

<br>

### 3.1 개념

모놀리틱에서 모든 기능이 **하나의 큰 공용 DB**를 함께 쓴다. MSA에서는 **각 서비스가 자기 DB를 독점**한다.

핵심 규칙은 하나다. **다른 서비스의 데이터는 그 서비스를 통해서만 얻는다.** 다른 서비스의 테이블에 직접 SELECT나 JOIN을 하면 안 된다.

<br>

### 3.2 그러면 JOIN은 어떻게 하나?

모놀리틱에서는 주문 목록을 상품명과 회원 이름을 JOIN으로 쉽게 붙였다. MSA의 주문 DB에는 주문 테이블만 있으므로, 상품명이 필요하면 아래 셋 중에 하나를 고른다.

<table>
    <thead>
        <th>방법</th>
        <th>설명</th>
        <th>언제 쓰나</th>
    </thead>
    <tbody>
        <tr>
            <td>① API 호출</td>
            <td>필요할 때 상품 서비스에 물어본다.</td>
            <td>항상 최신 값이 필요할 때</td>
        </tr>
        <tr>
            <td>② 스냅샷 저장</td>
            <td>주문하는 순간의 상품명을 주문 테이블에 같이 저장한다.</td>
            <td>"주문 당시"값을 보여주면 될 때</td>
        </tr>
        <tr>
            <td>③ 읽기 모델 복제</td>
            <td>상품 이벤트를 구독해서 필요한 컬럼만 내 DB에 복사해둔다.</td>
            <td>자주 조회하고 최신 값도 필요할 때</td>
        </tr>
    </tbody>
</table>

여기서 **"주문 내역에 주문 당시 상품명을 보여줄 것인가, 현재 상품명을 보여줄 것인가"** 는 기술이 아니라 **비즈니스 결정**이다. 당시 상품명이면 ②로 끝나고, 현재 상품명이면 ①이나 ③이 필요하다.

<br>

## 3.3 장점과 단점

<table>
    <thead>
        <th>장점</th>
        <th>단점</th>
    </thead>
    <tbody>
        <tr>
            <td>서비스별로 독립적으로 스키마를 바꾸고 배포할 수 있다.</td>
            <td>같은 데이터가 여러 DB에 중복되고, 이를 맞추는 동기화 작업이 생긴다.</td>
        </tr>
        <tr>
            <td>서비스 성격에 맞는 DB를 고를 수 있다. (폴리글랏 저장소)</td>
            <td>여러 서비스에 걸친 데이터 일관성을 애플리케이션 코드(Saga, 이벤트)로 직접 맞춰야 한다.</td>
        </tr>
        <tr>
            <td>한 DB의 장애가 다른 서비스로 번지지 않는다. (장애 격리)</td>
            <td>관리할 DB가 1개에서 수십 개로 늘어나 운영 부담이 커진다.</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

## 4. 서비스 성격에 맞는 저장소 고르기

<br>

DB를 서비스마다 따로 가지게 되면 "모든 서비스가 같은 DB를 써야 한다"는 제약도 사라진다. 그래서 데이터의 성격을 보고 저장소를 고른다.

<table>
    <thead>
        <th>저장소</th>
        <th>한 줄 특징</th>
        <th>잘 맞는 서비스</th>
    </thead>
    <tbody>
        <tr>
            <td>MySQL</td>
            <td>ACID 트랜잭션과 제약조건(외래 키, 유니크)으로 정합성을 지키는 표준 RDBMS</td>
            <td>주문, 결제, 회원처럼 틀리면 절대 안되는 데이터</td>
        </tr>
        <tr>
            <td>PostgreSQL</td>
            <td>RDBMS이면서 JSONB, 공간 데이터(PostGIS 확장) 같은 고급 기능을 지원. MVCC로 읽기와 쓰기가 서로 막지 않는다.</td>
            <td>정형 + 반정형 데이터가 섞인 서비스, 위치 기반 서비스</td>
        </tr>
        <tr>
            <td>MongoDB</td>
            <td>스키마를 미리 정하지 않고 JSON 같은 문서 단위로 저장. 서비스를 추가해 수평 확장하기 쉽다.</td>
            <td>상품 카탈로그, 전시, 이벤트/로그 수집</td>
        </tr>
        <tr>
            <td>Redis</td>
            <td>메모리에 저장해서 매우 빠르다. 다양한 자료구조, 만료 시간(TTL), 원자적 연산 지원</td>
            <td>캐시, 세션, 실시간 랭킹</td>
        </tr>
         <tr>
            <td>Elasticsearch</td>
            <td>문장을 단어(토큰)로 쪼개 저장하는 역색인 구조로 전문 검색에 강하다. 보통 1초 안에 검색에 반영되는 준실시간(Near Real-Time)</td>
            <td>통합 검색, 필터 검색, 로그 분석</td>
        </tr>
    </tbody>
</table>

<br>

## 4.1 정리하면서 바로 잡은 것
- **"NoSQL은 트랜잭션이 없다"라는 말은 반만 맞다.** MongoDB는 4.0부터 여러 문서에 걸친 트랜잭션을 지원한다. 다만 문서 하나에 관련 데이터를 함께 넣는 설계를 하면 트랜잭션이 필요한 경우 자체가 줄어든다.
- **Redis는 보통 "원본 DB"가 아니라 원본 앞의 캐시로 쓴다.** 먼저 Redis를 보고(Cache Hit), 없으면 원본 DB에서 가져와 Redis에 채워 넣는(Cache Miss) 방식이 대표적이다.

<br>
<hr>

## 5. 강한 일관성 vs 최종적 일관성

<br>

### 5.1 두 가지 일관성

<table>
    <thead>
        <th>구분</th>
        <th>강한 일관성 (Strong)</th>
        <th>최종적 일관성 (Eventual)</th>
    </thead>
    <tbody>
        <tr>
            <td>의미</td>
            <td>쓰자마자 누가 읽어도 최신 값</td>
            <td>잠깐은 옛날 값이 보일 수 있지만, 시간이 지나면 결국 모두 같아짐</td>
        </tr>
        <tr>
            <td>구현 수단</td>
            <td>하나의 DB 트랜잭션, 2PC</td>
            <td>이벤트, Saga, 재시도</td>
        </tr>
        <tr>
            <td>장애에 대한 태도</td>
            <td>참여자가 모두 살아 있어야 진행</td>
            <td>일부가 느리거나 멈춰도 나머지는 계속 동작</td>
        </tr>
        <tr>
            <td>어울리는 데이터</td>
            <td>잔액, 재고 차감</td>
            <td>통계, 검색 인덱스, 알림</td>
        </tr>
    </tbody>
</table>

MSA에서 서비스 간 데이터는 대부분 **최종적 일관성**으로 맞춘다. 그래서 "잠깐 어긋나는 동안 무슨 일이 생길 수 있는가"를 미리 알고 대비해야 한다. 그 대표적인 함정이 다음의 순서 뒤바뀜이다.

<br>

### 5.2 함정 : 이벤트 순서가 뒤바뀌면 ⭐

이벤트는 네트워크를 타고 이동하므로, **먼저 보낸 것이 먼저 도착한다는 보장이 없다.**

```aiignore
10:00:00  사용자 주문      → "주문 생성됨" 발행
10:00:05  사용자 바로 취소 → "주문 취소됨" 발행

결제 서비스 입장에서 도착 순서:
  ① "주문 취소됨" 도착  → 결제 상태 = 취소
  ② "주문 생성됨" 도착  → 결제 상태 = 결제 대기   ← 취소를 덮어써 버림!
```

사용자는 분명 취소했는데 결제 서비스에는 "결제 대기"가 남는다.  이렇게 늦게 도착한 옛날 이벤트가 최신 상태를 덮어쓰는 것이 **데이터 오염**이다.

<br>

### 5.3 해결 전략 네 가지

<table>
    <thead>
        <th>전략</th>
        <th>방법</th>
        <th>핵심</th>
    </thead>
    <tbody>
        <tr>
            <td>A. 같은 키로 보내기</td>
            <td>Kafka 메시지 키를 주문 ID로 지정</td>
            <td>같은 키 → 같은 파티션 → 파티션 안에서는 순서 보장. 애초에 순서가 뒤바뀌지 않게 한다.</td>
        </tr>
        <tr>
            <td>B. 버전 비교</td>
            <td>이벤트에 버전(또는 발생 시각)을 넣고, 컨슈머는 저장된 버전보다 낮은 이벤트를 버린다.</td>
            <td>취소(v2)가 먼저 반영된 뒤 생성(v1)이 오면 1 < 2 이므로 무시한다.</td>
        </tr>
        <tr>
            <td>C. 상태 전이 검증</td>
            <td>허용된 상태 변화만 받아들인다.</td>
            <td>"PENDING 에서만 완료/취소 가능" 규칙이 있으면, 이미 CANCELLED인 주문에 늦게 온 이벤트는 절단된다.</td>
        </tr>
        <tr>
            <td>D. 원본 재조회</td>
            <td>이벤트 내용을 그대로 믿지 않고, 처리 직전에 원본 서비스의 현재 상태를 API로 확인한다.</td>
            <td>정확하지만 호출 비용이 든다.</td>
        </tr>
    </tbody>
</table>

**A는 예방, B~D는 도착한 뒤의 방어**다. 실무에서는 A로 대부분을 막고, B나 C로 한번 더 막는 식이다.

<br>
<hr>

## 6. 실패 관점에서 다시 보는 Saga : 보상 트랜잭션

<br>

Day 01에서 배운 Saga를 이번에는 **"중간에 실패하면 어떻게 되돌리는가"** 의 관점에서 다시 봤다.

<br>

### 6.1 원칙

Saga의 각 단계는 이미 **자기 DB에 커밋**된 상태다. 커밋된 데이터는 DB 롤백으로 되돌릴 수 없다. 그래서 **반대 효과를 내는 비즈니스 로직을 새로 실행**한다. 이것이 보상 트랜잭션이고, **성공한 단계만, 역순으로** 실행한다.

<br>

### 6.2 예시 : 결제까지 성공했는데 재고가 부족한 경우

```aiignore
[정방향]  T1 주문 생성(PENDING) ──> T2 결제 승인 ──> T3 재고 확인 ❌ 재고 부족
                                                          │
[보상]    C1 주문 CANCELLED  <──── C2 결제 취소(환불) <───┘
```

- T3는 실패했으므로 바뀐 데이터가 없다. 보상할 것이 없다.
- T2(결제)는 이미 승인되었으므로 **C2 결제 취소**가 필요하다.
- T1(주문)은 이미 저장되었으므로 **C1 주문 취소**가 필요하다.
- 실행 순서는 성공한 역순인 **C2 → C1**이다.

Day01에선 T2가 "조회만 하는 사용자 검증"이라 C1 하나로 끝났었지만, 이번 예제는 T2가 **실제로 돈이 움직인 단계**라서 보상이 하나 더 필요하다. **보상이 몇 개 필요한지는 "실패 직전까지 무엇이 실제로 커밋되었는가"** 로 결정된다.

<br>
<hr>

## 7. Kafka 이벤트 시스템 설계 ⭐⭐⭐

<br>

이번 수업에서 가장 어려웠던 부분이다. 용어가 따로 나와서 헷갈렸는데, 결국 모두 **"이벤트 하나를 유실 없이, 중복 없이 처리하려면?"** 이라는 한 가지 질문에 대한 답이었다.

<br>

### 7.0 먼저 큰 그림 : 이벤트 하나의 여행

주문이 생성되어 이벤트가 결제 서비스에 처리되기까지의 전체 경로이다. 7장의 각 절이 이 경로의 어느 지점을 다루는지 표시했다. 

```aiignore
 [주문 서비스]                                                 [결제 서비스]
 ┌──────────────────────────┐                                ┌────────────────────────┐
 │ ① 주문 저장               │                                │ ⑤ 이벤트 수신           │
 │ ② 같은 트랜잭션에서        │    ③ 릴레이가     ④ Kafka에    │ ⑥ 이미 처리한 이벤트?    │
 │    outbox 테이블에 이벤트  │──> outbox를 읽어 ──> 저장 ───> │    (eventId로 확인)     │
 │    기록                   │    Kafka로 발행                │ ⑦ 처리 후 오프셋 커밋    │
 └──────────────────────────┘                                └────────────────────────┘
    7.6 ~ 7.8 (Dual Write,          7.8 (릴레이 방식)              7.3 ~ 7.5 (전달 보장,
    Outbox)                                                     컨슈머 설정, 멱등성)

 이벤트의 모양(무엇을 담을지)은 7.1 ~ 7.2
```

<br>

### 7.1 이벤트 vs 커맨드

메시지에는 두 종류가 있다. **이미 일어난 일을 알리는 것**과 **해 달라고 시키는 것**이다.

<table>
    <thead>
        <th>구분</th>
        <th>이벤트 (Event)</th>
        <th>커맨드 (Command)</th>
    </thead>
    <tbody>
        <tr>
            <td>뜻</td>
            <td>"~가 일어났다" (사실)</td>
            <td>"~를 해라" (요청)</td>
        </tr>
        <tr>
            <td>이름 짓기</td>
            <td>과거형: OrderCreated, PaymentApproved</td>
            <td>명령형: ReserveStock, RefundPayment</td>
        </tr>
        <tr>
            <td>받는 쪽</td>
            <td>몇 명이 받는지 모른다. (0명일 수도)</td>
            <td>정해진 한 곳</td>
        </tr>
        <tr>
            <td>거절</td>
            <td>못 한다. 이미 일어난 일이다.</td>
            <td>받는 쪽이 거절할 수 있다.</td>
        </tr>
        <tr>
            <td>주로 쓰는 곳</td>
            <td>Choreography Saga, 읽기 모델 갱신</td>
            <td>Orchestration Saga</td>
        </tr>
    </tbody>
</table>

비유하면 이벤트는 **신문 기사**("어제 주문 1건이 생성되었다."), 커맨드는 **업무 지시 메일**("재고팀, 상품A 2개 확보해주세요")이다. 신문은 누가 읽을지 모르고 내용을 거부할 수 있지만, 업무 지시는 받는 사람이 "재고가 업어서 못 합니다"라고 거절할 수 있다.

<br>

### 7.2 이벤트에 얼마나 담을까

<table>
    <thead>
        <th>방식</th>
        <th>담는 것</th>
        <th>장점</th>
        <th>단점</th>
    </thead>
    <tbody>
        <tr>
            <td>이벤트 알림 (Event Notification)</td>
            <td>ID정도만</td>
            <td>이벤트가 작고, 원본이 바뀌어도 이벤트 형식은 거의 안 바뀐다.</td>
            <td>받는 쪽이 상세 정보를 얻으려고 원본 서비스를 다시 호출해야 한다. 웝노이 죽어 있으면 처리 불가</td>
        </tr>
        <tr>
            <td>상태 전달 (Event-Carried State Transfer)</td>
            <td>처리에 필요한 데이터까지</td>
            <td>받는 쪽이 원본에 묻지 않고 혼자 처리할 수 있다.</td>
            <td>이벤트가 커지고, 필드 구조가 바뀔 때 버전 관리가 필요하다.</td>
        </tr>
    </tbody>
</table>

두 이름은 Martin Fowler가 이벤트 기반 시스템의 유형을 정리하면서 붙인 것이다. MSA에서는 서비스끼리 덜 의존하도록 **상태 전달** 방식을 많이 쓴다. 그래서 다음 절의 `eventVersion` 같은 필드가 필요해진다.

<br>

## 7.3 이벤트의 "봉투" 구조

택배 상자를 떠올리면 쉽다. **송장(메타데이터)** 과 **내용물(payload)** 로 나뉜다. 송장만 보고도 "누가, 언제, 어떤 종류의 물건을, 몇 번째로 보냈는지" 알 수 있어야 한다.

```aiignore
{
  "eventId": "9c1e7a52-...",
  "eventType": "ORDER_CANCELLED",
  "eventVersion": 1,
  "aggregateType": "ORDER",
  "aggregateId": "42",
  "occurredAt": "2026-11-12T10:00:05",
  "payload": { "orderId": 42, "userId": 3, "reason": "고객 변심" }
}
```

<table>
    <thead>
        <th>필드</th>
        <th>송장으로 치면</th>
        <th>왜 필요한가</th>
    </thead>
    <tbody>
        <tr>
            <td>eventId</td>
            <td>운송장 번호</td>
            <td>이벤트마다 유일한 번호. 컨슈머가 "이 번호 이미 처리했나?"를 확인하는 멱등 키가 된다.</td>
        </tr>
        <tr>
            <td>eventType</td>
            <td>물품 종류</td>
            <td>같은 토픽 안에서 생성/취소 등을 구분해 처리 로직을 나눈다.</td>
        </tr>
        <tr>
            <td>eventVersion</td>
            <td>상자 규격 버전</td>
            <td>payload 구조가 바뀌어도 옛날 컨슈머가 깨지지 않게 버전을 표시한다.</td>
        </tr>
        <tr>
            <td>aggregateType / aggregateId</td>
            <td>받는 사람 주소</td>
            <td>어느 대상에 대한 이벤트인가. aggregateId는 Kafka 메시지 키로도 쓴다. → 같은 주문의 이벤트는 같은 파티션으로 순서 보장</td>
        </tr>
        <tr>
            <td>occurredAt</td>
            <td>발송 일시</td>
            <td>순서 판단과 디버깅</td>
        </tr>
        <tr>
            <td>payload</td>
            <td>내용물</td>
            <td>실제 비즈니스 데이터</td>
        </tr>
    </tbody>
</table>

<br>

### 7.4 전달 보장 (Delivery Semantics)

**메시지가 몇 번 처리되는지는 "처리"와 "오프셋 커밋" 중 무엇을 먼저 하느냐로 결정된다.**

Kafka 설계 문서도 같은 방식으로 설명한다. 컨슈머가 메시지를 읽고 **위치를 먼저 저장한 뒤 처리**하면 at-most-once, **처리한 뒤 위치를 저장**하면 at-least-once가 된다.

#### 택배 수령 서명으로 이해하기

- **서명부터 하고 상자를 연다. (커밋 → 처리)** : 서명하고 상자를 열려는 순간 쓰러지면, 회사 기록에는 "수령 완료"인데 물건은 확인하지 못했다. 다시 배달되지 않는다. **→ 유실**
- **상자를 열어 확인한 뒤 서명한다. (처리 → 커밋)** : 확인은 다 했는데 서명 직전에 쓰러지면, 기록에는 "미수령"이라는 같은 택배가 한 번 더 온다. **→ 중복**

<table>
    <thead>
        <th>보장 순서</th>
        <th>순서</th>
        <th>장애가 나면</th>
        <th>어울리는 곳</th>
    </thead>
    <tbody>
        <tr>
            <td>At-Most-Once (최대 한 번)</td>
            <td>커밋 → 처리</td>
            <td>유실될 수 있다.</td>
            <td>일부 빠져도 괜찮은 지표 수집</td>
        </tr>
        <tr>
            <td>At-Least-Once (최소 한 번)</td>
            <td>처리 → 커밋</td>
            <td>중복될 수 있다.</td>
            <td>대부분의 비즈니스 이벤트 (권장)</td>
        </tr>
        <tr>
            <td>Exactly-Once (정확히 한 번)</td>
            <td>Kafka 트랜잭션으로 읽기/쓰기/커밋을 한 번에</td>
            <td>유실도 중복도 없다.</td>
            <td>Kafka → Kafka로 이어지는 스트림 처리 (Kafka Stream)</td>
        </tr>
    </tbody>
</table>

<br>

#### Exactly-Once면 다 해결되는 것 아닌가?

여기서 헷갈렸다. 공식 문서를 보면 Exactly-Once는 **Kafka 안에서 읽고 → 가공해서 → Kafka에서 다시 쓰는** 흐름에 해당한다. 결제 서비스처럼 이벤트를 받아서 **MySQL에 저장**하는 경우에는 "DB 저장"과 "Kafka 오프셋 커밋"이 서로 다른 시스템이라 Kafka 트랜잭션에 하나로 묶이지 않는다.

그래서 실무의 현실적인 조합은 다음과 같다.

> **At-Least-Once (유실은 절대 없게) + 멱등한 컨슈머 (중복은 받는 쪽이 걸러내게)**

"중복은 걸러낼 수 있지만, 유실은 되돌릴 수 없다." 그래서 유실보다 중복을 택한다.

<br>

### 7.5 컨슈머 설정과 멱등성

<table>
    <thead>
        <th>설정</th>
        <th>실습 값</th>
        <th>Kafka 기본값</th>
        <th>의미</th>
    </thead>
    <tbody>
        <tr>
            <td>group-id</td>
            <td>order-api</td>
            <td>없음</td>
            <td>같은 그룹끼리 파티션을 나눠 읽는다.</td>
        </tr>
        <tr>
            <td>auto-offset-reset</td>
            <td>earliest</td>
            <td>latest</td>
            <td>이 그룹이 처음 구독할 때(커밋된 오프셋이 없을 때) 어디서부터 읽을지. earliest는 토픽의 처음부터, latest는 지금 이후부터</td>
        </tr>
        <tr>
            <td>enable-auto-commit</td>
            <td>false</td>
            <td>true</td>
            <td>true면 백그라운드에서 일정 주기(기본 5초)로 자동 커밋한다. 처리가 끝나기 전에 커밋될 수 있어서 끈다.</td>
        </tr>
        <tr>
            <td>listener.ack-mode</td>
            <td>manual_immediate</td>
            <td>(Spring Kafka 기본 BATCH)</td>
            <td>코드에서 직접 "처리 끝!"이라고 알릴 때 (acknowledge()) 바로 커밋한다.</td>
        </tr>
    </tbody>
</table>

<br>

#### 왜 auto-commit을 끄는가

자동 커밋은 **시간 기준**으로 커밋한다. "5초 지났으면 지금까지 받은 것 커밋"이다. 처리가 끝났는지는 보지 않는다. 그러면 처리 중에 커밋이 먼저 일어나고, 그 직후 컨슈머가 죽으면 **커밋 → 처리** 순서가 되어 at-most-once(유실)처럼 동작할 수 있다. 그래서 자동 커밋을 끄고 **처리가 끝난 시점에 직접** 커밋한다.

<br>

**MANUAL vs MANUAL_IMMEDIATE**

Spring Kafka 공식 API 문서 기준으로 둘 다 "리스너가 직접 ack를 호출하는" 방식이다. 차이는 커밋 시점이다.
- **MANUAL :** ack를 모아 두었다가, 이번에 가져온(poll) 메시지를 **전부 처리한 뒤** 한꺼번에 커밋한다.
- **MANUAL_IMMEDIATE :** 리스너 스레드에서 ack를 호출하면 **그 즉시** 커밋한다.

메시지 하나하나의 처리 완료를 바로 기록하고 싶을 때 MANUAL_IMMEDIATE를 쓴다.

<br>

#### 멱등한 컨슈머 (Idempotent Consumer)

**멱등 (Idempotent) = 같은 일을 여러 번 해도 결과가 한 번 한 것과 같은 것.** 엘리베이터 버튼을 여러번 눌러도 엘리베이터가 한 번만 오는 것과 같다.

microservices.io의 Idempotent Consumer 패턴은 다음처럼 구현하라고 안내한다.

1. 처리한 메시지의 ID를 DB에 기록한다. (예: `processed_events` 테이블)
2. 이 기록은 **비즈니스 데이터 변경과 같은 DB 트랜잭션**에서 한다.
3. 같은 ID가 다시 오면 기본 키 중복으로 저장이 실패하므로, 이미 처리한 것으로 보고 건너뛴다.

```aiignore
이벤트(eventId=9c1e...) 도착
   │
   ├─ processed_events에 9c1e... 있음? ── 예 ──> 아무것도 안 하고 커밋만 (중복 무시)
   │
   └─ 아니오 ──> [한 트랜잭션] 결제 상태 변경 + processed_events에 9c1e... 기록
                 └─> 오프셋 커밋
```

`eventId`가 바로 이 용도로 들어 있는 것이다.

<br>

### 7.6 Dual Write 문제 : 왜 Outbox가 필요한가

여기서부터가 Outbox가 등장하는 이유다. 지금까지는 **받는 쪽**(컨슈머) 이야기였고, 이제는 **보내는 쪽**(주문 서비스) 이야기다.

주문 서비스는 두 가지를 해야 한다.

1. 주문을 **DB에 저장**한다.
2. "주문 생성됨" 이벤트를 **Kafka에 발행**한다.

문제는 **DB와 Kafka가 서로 다른 시스템이라 둘을 하나의 트랜잭션으로 묶을 수 없다**는 것이다. 이렇게 서로 다른 두 곳에 각각 쓰는 것을 **Dual Write(이중 쓰기)** 라고 한다. 어떤 순서로 해도 구멍이 생긴다.

<table>
    <thead>
        <th>시나리오</th>
        <th>무슨 일이 생기나</th>
        <th>결과</th>
    </thead>
    <tbody>
        <tr>
            <td>DB 저장 성공 → Kafka 발행 실패 (Kafka 장애, 네트워크 오류)</td>
            <td>주문은 있는데 이벤트가 없다.</td>
            <td>결제가 시작되지 않아 주문이 영원히 PENDING</td>
        </tr>
        <tr>
            <td>Kafka 발행 성공 → DB 커밋 실패 (롤백)</td>
            <td>이벤트는 나갔는데 주문이 없다.</td>
            <td>다른 서비스가 존재하지 않는 주문(유령 주문)을 처리</td>
        </tr>
        <tr>
            <td>DB 커밋 전에 Kafka 발행</td>
            <td>이벤트가 너무 빨리 도착한다.</td>
            <td>컨슈머가 주문을 조회했을 때 아직 커밋 전이라 "없음"으로 처리</td>
        </tr>
    </tbody>
</table>

`@Transactional` 메서드 안에서 Kafka로 보내도 해결되지 않는다. `@Transactional`이 묶어주는 것은 **DB 작업뿐**이고, Kafka로 이미 나간 메시지는 DB가 롤백되어도 회수되지 않는다.

microservices.io는 이 문제를 이렇게 정리한다. **"서비스가 DB를 갱신하는 것과 메시지를 보내는 것을 어떻게 원자적으로 할 수 있는가?"** 2PC는 DB나 브로커가 지원하지 않을 수 있고, 서비스를 두 시스템에 강하기 묶기 때문에 답이 아니다.

<br>

### 7.7 Transactional Outbox 패턴 ⭐

#### 아이디어 한 줄
**"Kafka에 바로 보내지 말고, 보낼 이벤트를 주문과 같은 DB에 같은 트랜잭션으로 적어 두자. 보내는 건 나중에 다른 누군가가 한다."**

두 시스템(DB + Kafka)에 쓰이는 문제를 **한 시스템(DB)에 쓰는 문제로 바꾼 것**이 핵심이다. 같은 DB 안이라면 `@Transactional` 하나로 묶을 수 있다.

#### 비유 : 가게 장부의 "보낼 알림" 칸
> 동네 가게 사장님이 주문을 받으면 장부에 적고, 단골들에게 "새 주문이 들어왔어요." 문자를 보내야 한다.
> 
> **Dual Write 방식 :** 장부에 적고 → 휴대폰으로 문자를 보낸다. 그런데 장부에 적자마자 휴대폰이 꺼지면 문자는 영영 안 나간다. 반대로 문자부터 보냈는데 손님이 "아, 주문 취소할게요."해서 장부에 안 적으면, 단골들은 없는 주문 소식을 받는다.
> **Outbox 방식 :** 장부 한 줄에 **주문 내용**과 **"보낼 문자: 새 주문이 들어왔어요 [] 미발송"** 을 **같이** 적는다. 한 줄을 적거나 아예 안 적거나 둘 중 하나이므로, 주문만 있고 문자 기록이 없는 일은 생기지 않는다.
> 
> 문자는 **아르바이트생(릴레이)** 이 담당한다. 아르바이트생은 몇 초마다 장부를 훑어서 "미발송"인 줄을 찾고, 문자를 보낸 뒤 **[v] 발송 완료** 로 체크한다. 휴대폰이 꺼져 있으면 체크하지 않고 두었다가 다음에 다시 시도한다.
> 
> 단, 아르바이트생이 문자를 보낸 직후 체크하기 전에 자리를 비우면, 다음 사람이 그 줄을 "미발송"으로 보고 **같은 문자를 한번 더** 보낸다. 그래서 단골들은 **문자 번호를 보고 이미 받은 문자면 무시**한다.

비유와 실제를 대응시키면 다음과 같다.

<table>
    <thead>
        <th>비유</th>
        <th>실제</th>
    </thead>
    <tbody>
        <tr>
            <td>장부</td>
            <td>주문 서비스의 DB</td>
        </tr>
        <tr>
            <td>같은 줄에 주문 + 보낼 문자</td>
            <td>같은 트랜잭션으로 orders와 outbox_events에 INSERT</td>
        </tr>
        <tr>
            <td>[ ] 미발송 / [v] 발송 완료</td>
            <td>outbox의 status : PENDING / PUBLISHED</td>
        </tr>
        <tr>
            <td>아르바이트생</td>
            <td>메시지 릴레이 (Relay)</td>
        </tr>
        <tr>
            <td>휴대폰이 꺼짐</td>
            <td>Kafka 장애</td>
        </tr>
        <tr>
            <td>같은 문자를 한번 더 받음</td>
            <td>At-Least-Once 에 따른 중복 발생</td>
        </tr>
        <tr>
            <td>문자 번호를 보고 무시</td>
            <td>멱등한 컨슈머 (eventId로 확인)</td>
        </tr>
    </tbody>
</table>

<br>

### 단계별로 따라가기

#### ① 주문 서비스 (한 트랜잭션)

```aiignore
BEGIN
  INSERT INTO orders        (...)                       ← 주문 저장
  INSERT INTO outbox_events (event_type, aggregate_id,
                             payload, status='PENDING') ← 보낼 이벤트 저장
COMMIT   ← 둘 다 저장되거나, 둘 다 저장되지 않는다
```

여기서 주문 서비스의 일은 끝이다. **Kafka가 살아 있는지 신경 쓰지 않는다.**


#### ② 릴레이 (별도로 주기적으로 실행)

```aiignore
1. outbox_events에서 status = 'PENDING' 인 행을 오래된 순으로 가져온다.
2. 각 행을 Kafka로 발행한다. (메시지 키 = aggregate_id)
3. 성공하면 status = 'PUBLISHED'로 변경
   실패하면 attempt_count + 1, last_error 기록 → 다음 주기에 다시 시도
```

#### ③ 컨슈머 (결제 서비스)

멱등한 컨슈머 그대로다. `eventId`로 중복을 거른다.

<br>

### 장애 상황별로 확인하기

<table>
    <thead>
        <th>상황</th>
        <th>Outbox에서는</th>
    </thead>
    <tbody>
        <tr>
            <td>주문 저장 오류로 롤백</td>
            <td>outbox 행도 함께 롤백된다. → 이벤트가 나가지 않는다. → 유령 주문 없음</td>
        </tr>
        <tr>
            <td>주문 커밋 후 Kafka가 죽어 있음</td>
            <td>outbox 행이 PENDING으로 남아 있다 → 릴레이가 계속 재시도 → kAFKA 복구 후 실행 → 유실 없음</td>
        </tr>
        <tr>
            <td>이벤트가 커밋 전에 도착</td>
            <td>릴레이는 커밋된 행만 읽을 수 있다. → 이벤트는 항상 커밋 이후에 나간다. → "없음" 오판 없음</td>
        </tr>
        <tr>
            <td>릴레이가 발행 직후, PUBLISHED로 바꾸기 전에 죽음</td>
            <td>행이 여전히 PENDING → 재시작 후 같은 이벤트를 다시 발행 → 컨슈머가 eventId로 무시 → 중복은 받는 쪽이 처리</td>
        </tr>
    </tbody>
</table>

마지막 줄이 중요하다. **Outbox는 유실을 없애 주지만 중복까지 없애 주지는 않는다.** microservices.io도 릴레이가 같은 메시지를 두 번 이상 발행할 수 있으므로 **컨슈머가 멱등해야 한다**고 명시한다. 그래서 Outbox와 멱등한 컨슈머는 **항상 짝**으로 기억해야 한다.

#### 실습에서 본 outbox 테이블 변화

Kafka를 잠깐 멈췄다가 다시 켠 실습에서는 outbox 테이블은 이렇게 바뀌었다.

<table>
    <thead>
        <th>id</th>
        <th>event_type</th>
        <th>aggregate_id</th>
        <th>status</th>
        <th>attempt_count</th>
        <th>last_error</th>
    </thead>
    <tbody>
        <tr>
            <td>1</td>
            <td>ORDER_CREATED</td>
            <td>1</td>
            <td>PUBLISHED</td>
            <td>0</td>
            <td>(없음)</td>
        </tr>
        <tr>
            <td>2</td>
            <td>ORDER_CREATED</td>
            <td>2</td>
            <td>PENDING</td>
            <td>4</td>
            <td>TimeoutException (Kafka 멈춘 동안 실패할 때마다 +1)</td>
        </tr>
        <tr>
            <td>2</td>
            <td>ORDER_CREATED</td>
            <td>2</td>
            <td>PUBLISHED</td>
            <td>4</td>
            <td>TimeoutException (Kafka 재시작 후 발행 성공, 이력은 남김)</td>
        </tr>
    </tbody>
</table>

`attempt_count`는 **실패한 횟수**다. 발행에 성공해도 `last_error`를 지우지 않아서, 나중에 "이 이벤트가 몇 번 막혔었는지" 추적할 수 있다.

<br>

### 7.8 릴레이를 구현하는 두 가지 방법

microservices.io는 릴레이 구현 방법으로 두 패턴을 소개한다.

<table>
    <thead>
        <th>방식</th>
        <th>동작</th>
        <th>장점</th>
        <th>단점</th>
    </thead>
    <tbody>
        <tr>
            <td>Polling Publisher (실습 방식)</td>
            <td>스케줄러가 주기적으로 outbox 테이블을 SELECT해서 발행</td>
            <td>어떤 SQL DB에서도 동작하고 구현이 간단하다.</td>
            <td>폴링 주기만큼 지연된다. 순서를 지키며 발행하기 까다롭다. DB에 주기적인 조회 부하가 생긴다.</td>
        </tr>
        <tr>
            <td>Transaction Log Tailing (CDC)</td>
            <td>DB의 변경 로그(MySQL binlog, PostgreSQL WAL)을 읽어서 outbox에 INSERT된 행을 발행</td>
            <td>지연이 짧고, 애플리케이션에 폴링 코드가 필요 없다.</td>
            <td>DB별 전용 도구가 필요하고 운영할 인프라가 늘어난다. 중복 발행을 완전히 피하기는 어렵다.</td>
        </tr>
    </tbody>
</table>

<br>

### Polling Publisher를 여러 대 띄우면?

주문 서비스 인스턴스가 3대면 릴레이 스케줄러도 3개가 동시에 돈다. 그러면 **같은 PENDING 행을 셋이 동시에 가져가 3번 발행** 할 수 있다. 막는 방법은 두 가지다.

- `SELECT ... FOR UPDATE SKIP LOCKED` : PostgreSQL 공식 문서는 SKIP LOCKED를 "즉시 잠글 수 없는 행은 건너 뛴다"고 설명하며, 여러 소비자가 **큐처럼 쓰는 테이블**에 접근할 때 잠금 경합을 피하는 용도로 쓸 수 있다고 안내한다.
- **ShedLock 같은 분산 락 :** 스케줄 작업 자체를 한 번에 한 인스턴스만 실행되게 막는다.

다만 여러 릴레이가 행을 나눠 가지면 같은 주문의 이벤트가 서로 다른 릴레이에서 발행되어 **순서가 뒤바뀔 위험**이 생긴다. 

<br>

### 7.9 퍼즐 맞추기 : 각 장치가 막는 구멍

<table>
    <thead>
        <th>문제</th>
        <th>막는 장치</th>
        <th>위치</th>
    </thead>
    <tbody>
        <tr>
            <td>DB에는 저장됐는데 이벤트가 안 나감 (유실)</td>
            <td>Transactional Outbox</td>
            <td>보내는 쪽</td>
        </tr>
        <tr>
            <td>DB는 롤백됐는데 이벤트가 나감 (유령)</td>
            <td>Transactional Outbox</td>
            <td>보내는 쪽</td>
        </tr>
        <tr>
            <td>Kafka 장애 동안 이벤트가 사라짐</td>
            <td>Outbox의 PENDING + 릴레이 재시도</td>
            <td>보내는 쪽</td>
        </tr>
        <tr>
            <td>처리 전에 커밋되어 이벤트를 놓침</td>
            <td>auto-commit 끄기 + 처리 후 수동 커밋 (At-Least-Once)</td>
            <td>받는 쪽</td>
        </tr>
        <tr>
            <td>같은 이벤트가 두 번 처리됨</td>
            <td>멱등한 컨슈머 (eventId)</td>
            <td>받는 쪽</td>
        </tr>
        <tr>
            <td>같은 주문의 이벤트 순서가 뒤바뀜</td>
            <td>메시지 키 = aggregateId, 버전 비교, 상태 전이 검증</td>
            <td>양쪽</td>
        </tr>
        <tr>
            <td>이벤트 구조가 바뀌어 옛 컨슈머가 깨짐</td>
            <td>eventVersion</td>
            <td>이벤트 설계</td>
        </tr>
    </tbody>
</table>

**보내는 쪽은 Outbox로 "최소 한 번은 반드시 보낸다"를 보장하고, 받는 쪽은 멱등성으로 "두 번 와도 한 번만 반영한다"를 보장한다.** 이 둘이 만나서 결과적으로 "정확히 한 번 반영된 것처럼" 동작한다. 

<br>
<hr>

## 8. 핵심 정리

- **DDD는 구조, BDD는 합의, TDD는 구현**을 담당하는 서로 보완적인 기법이다.
- **Eureka**는 서비스 이름으로 인스턴스 목록을 알려주는 레지스트리이다. 클라이언트가 목록을 캐시하므로 반영이 늦을 수 있어 타임아웃, 재시도, 서킷 브레이커를 함께 설계한다.
- **Database per Service** 에서는 다른 서비스 DB를 직접 조회하지 않는다. 필요한 데이터는 API 호출, 스냅샷, 이벤트 복제 중에서 고른다.
- 서비스 성격에 따라 DB를 골라 쓴다.
- MSA는 대부분 **최종적 일관성**으로 데이터를 맞추며, 이때 **이벤트 순서 뒤바뀜**을 메시지, 키 버전 비교, 상태 전이 검증, 원본 재조회로 막는다.
- **보상 트랜잭션**은 롤백이 아니라 반대 작업이며, 실패 직전까지 **커밋된 단계만 역순으로** 실행한다.
- 이벤트는 **과거형 사실**, 커맨드는 **명령어 요청이다.** 
- 전달 보장은 **처리와 오프셋 커밋의 순서**로 정해진다. 실무 기본값은 **At-Least-Once + 멱등한 컨슈머** 다.
- DB와 Kafka에 따로 쓰는 **Dual Write**는 유실이나 유령 이벤트를 만든다. **Transactional Outbox**는 이벤트를 같은 DB 트랜잭션으로 기록하고 릴레이가 나중에 발행하게 해서 이를 막는다.
- Outbox는 유실을 없애지만 **중복은 남는다.** 그래서 **Outbox와 멱등한 컨슈머는 항상 짝**이다.

<br>

