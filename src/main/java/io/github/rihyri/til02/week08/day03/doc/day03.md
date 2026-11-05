<br>

# Week 08 - Day 03. 단위 테스트와 API 테스트

<br>

## 1. 단위 테스트 (Unit Test)

<br>

### 1.1 단위 테스트란?

단위 테스트(Unit Test)는 애플리케이션을 구성하는 작은 단위의 코드가 의도한 대로 동작하는지 검증하는 테스트이다.

일반적으로 메서드나 클래스를 대상으로 하며, 특정 기능의 결과가 예상한 값과 일치하는지 확인한다.

단위 테스트에서 중요한 개념은 **고립(Isolation)** 이다.

테스트 대상이 데이터베이스, 네트워크 또는 다른 컴포넌트에 의존하면 테스트 결과가 외부 환경에 영향을 받을 수 있다.

따라서 테스트 대상의 외부 의존성을 Mock 객체로 대체함으로써 비즈니스 로직에 집중할 수 있다.

<br>

### 1.2 단위 테스트의 장점

- **버그 조기 발견** : 기능 구현 후 예상하지 못한 오류를 빠르게 발견할 수 있다.
- **안전한 리팩토링** : 기존 기능이 정상 동작하는지 확인하면서 코드를 개선할 수 있다.
- **유지보수성 향상** : 테스트를 통해 기능의 요구사항과 기대 결과를 파악할 수 있다.
- **빠른 피드백** : 외부 시스템을 사용하지 않으면 테스트를 빠르게 실행할 수 있다.

<br>
<hr>

## 2. JUnit 5

<br>

`JUnit`은 Java 애플리케이션의 테스트를 작성하고 실행하기 위한 테스트 프레임워크이다.

Spring Boot 프로젝트에서는 주로 JUnit Jupiter를 이용해 테스트를 작성한다.

<br>

### 2.1 주요 어노테이션

<table>
    <thead>
        <th>어노테이션</th>
        <th>설명</th>
    </thead>
    <tbody>
        <tr>
            <td>@Test</td>
            <td>테스트 메서드 지정</td>
        </tr>
        <tr>
            <td>@DisplayName</td>
            <td>테스트의 목적을 읽기 쉬운 문장으로 표시</td>
        </tr>
        <tr>
            <td>@BeforeEach</td>
            <td>각 테스트 메서드 실행 전 호출</td>
        </tr>
        <tr>
            <td>@AfterEach</td>
            <td>각 테스트 메서드 실행 후 호출</td>
        </tr>
        <tr>
            <td>@BeforeAll</td>
            <td>테스트 클래스의 모든 테스트 실행 전 한 번 호출</td>
        </tr>
        <tr>
            <td>@AfterAll</td>
            <td>테스트 클래스의 모든 테스트 실행 후 한 번 호출</td>
        </tr>
        <tr>
            <td>@Disabled</td>
            <td>테스트 실행 제외</td>
        </tr>
    </tbody>
</table>

<br>

### 2.2 테스트 작성 패턴

#### · AAA 패턴

AAA는 Arrange, Act, Assert의 약자이다.

1. `Arrange` : 테스트에 필요한 데이터와 조건을 준비한다.
2. `Act` : 테스트 대상 메서드를 실행한다.
3. `Assert` : 실행 결과가 기대한 값과 일치하는지 확인한다.

예를 들어 상품의 재고 감소 기능을 테스트한다면 상품을 준비하고, 재고 감소 메서드를 호출한 뒤, 남은 재고를 확인할 수 있다.

<br>

#### · Given-When-Then 패턴

Given-When-Then은 테스트를 요구사항 형태로 표현하는 방식이다.

- `Given` : 어떤 상황이 주어졌을 때
- `When` : 특정 행동을 실행하면
- `Then` : 예상한 결과가 나타나야 한다.

AAA와 GWT는 역할이 유사하지만, GWT는 비즈니스 요구사항을 자연스럽게 표현하는 데 유용하다.

<br>

### 2.3 테스트 메서드 네이밍

테스트 메서드는 어떤 조건에서 어떤 결과를 기대하는지 명확하게 작성하는 것이 좋다.

```aiignore
예시:
process_shouldSucceed_whenProductInStock()
```

이는 상품의 재고가 충분할 때 주문 처리가 성공해야 한다는 의미이다.

테스트 이름만 읽어도 테스트의 목적을 이해할 수 있도록 작성하는 것이 중요하다.

<br>
<hr>

## 3. Mockito를 활용한 단위 테스트

<br>

`Mockito`는 Java에서 Mock 객체를 생성하고 동작을 설정할 수 있도록 지원하는 라이브러리이다.

Mock 객체는 실제 객체를 대신하는 테스트용 객체이며, 테스트 대상이 의존하는 컴포넌트의 동작을 제어하는 데 사용한다.

<br>

### 3.1 Mockito를 사용하는 이유

예를 들어 `OrderProcessService`가 `ProductRepository`를 통해 상품을 조회한다고 가정한다.

실제 Repository를 사용하면 데이터베이스 연결과 상품 데이터가 필요하다.

반면 Mockito를 사용하면 실제 DB 없이도 원하는 상품 데이터를 반환하도록 설정할 수 있다.

이를 통해 외부 환경과 관계없이 주문 처리 로직을 독립적으로 검증할 수 있다.

<br>

### 3.2 주요 어노테이션

<table>
    <thead>
        <th>어노테이션</th>
        <th>설명</th>
    </thead>
    <tbody>
        <tr>
            <td>@ExtendWith(MockitoExtension.class)</td>
            <td>JUnit5와 Mockito 연동</td>
        </tr>
        <tr>
            <td>@Mock</td>
            <td>가짜 객체 생성</td>
        </tr>
        <tr>
            <td>@InjectMocks</td>
            <td>테스트 대상 객체 생성 및 Mock 의존성 주입</td>
        </tr>
        <tr>
            <td>@BeforeEach</td>
            <td>테스트마다 필요한 객체와 데이터 초기화</td>
        </tr>
    </tbody>
</table>

`@InjectMocks`는 Spring의 `@Autowired`와 동일한 어노테이션이 아니다.

`@Autowired`는 Spring 컨테이너가 관리하는 Bean을 주입하지만, `@InjectMocks`는 Mockito가 Mock 객체를 테스트 대상에 주입하는 기능이다.

또한 Mockito 테스트에서는 일반적으로 Spring Context를 실행하지 않는다.

<br>

### 3.3 Stubbing: when()과 thenReturn()

Stubbing은 Mock 객체의 특정 메서드가 호출되었을 때 어떤 값을 반환할지 미리 설정하는 작업이다.

```aiignore
예시:

when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
```

이 설정은 `findById(1L)`이 호출될 때 실제 DB를 조회하지 않고 `testProduct`를 반환하도록 한다.

자주 사용하는 메서드는 다음과 같다.

- `thenReturn()` : 지정한 값 반환
- `thenThrow()` : 지정한 예외 발생
- `thenAnswer()` : 전달받은 인자 등을 이용해 반환 동작 정의

<br>

### 3.4 verify(): 메서드 호출 검증

`verify()`는 Mock 객체의 특정 메서드가 호출되었는지 확인한다.

```aiignore
예시:

verify(productRepository).findById(1L);
```

대표적인 검증 방법은 다음과 같다.

<table>
    <thead>
        <th>메서드</th>
        <th>설명</th>
    </thead>
    <tbody>
        <tr>
            <td>verify(mock)</td>
            <td>메서드가 한 번 호출되었는지 확인</td>
        </tr>
        <tr>
            <td>times(n)</td>
            <td>지정한 횟수만큼 호출되었는지 확인</td>
        </tr>
        <tr>
            <td>never()</td>
            <td>호출되지 않았는지 확인</td>
        </tr>
        <tr>
            <td>atLeastOnce()</td>
            <td>한 번 이상 호출되었는지 확인</td>
        </tr>
        <tr>
            <td>verifyNoInteractions()</td>
            <td>Mock 객체와 상호작용이 없었는지 확인</td>
        </tr>
    </tbody>
</table>

`assertThat()`이 값이나 상태를 검증한다면, `verify()`는 메서드 호출 여부를 검증한다고 이해할 수 있다.

다만 호출 여부만 확인하면 실제 결과가 올바른지 놓칠 수 있으므로 필요한 상태 검증도 함께 작성하는 것이 좋다.

<br>

### 3.5 Argument Matcher

Mockito에서는 메서드 인자를 유연하게 비교할 수 있도록 Argument Matcher를 제공한다.

- `any()` : 임의의 객체 인자와 매칭
- `anyLong()` : 임의의 Long 타입 값과 매칭
- `anyList()` : 임의의 비 null List와 매칭
- `eq()` : 지정한 값과 동일한 인자와 매칭

예를 들어 `verify(orderRepository).save(any(Order.class))`는 `Order` 객체를 인자로 받아 `save()`가 실행되었는지 검증한다.

<br>
<hr>

## 4. 좋은 단위 테스트 작성 방법

<br>

### 4.1 성공 케이스 테스트

정상적인 입력값이 주어졌을 때 기대한 결과가 반환되는지 확인한다.

예를 들어 재고가 10개인 상품을 2개 주문하면 재고가 8개로 감소하고, 총 주문 금액이 정상적으로 계산되어야 한다.

<br>

### 4.2 실패 케이스 테스트

잘못된 입력값 또는 비즈니스 규칙 위반 상황에서 적절한 예외가 발생하는지 검증한다.

예시:

- 재고보다 많은 수량을 주문한 경우
- 존재하지 않는 상품을 요청한 경우
- 주문 상품 목록이 비어 있는 경우

<br>

### 4.3 경계값 테스트

경계값 테스트는 입력값이 허용 범위의 끝에 있을 때 발생할 수 있는 오류를 확인한다.

재고가 10개라면 다음과 같이 테스트할 수 있다.

<table>
    <thead>
        <th>주문 수량</th>
        <th>기대 결과</th>
    </thead>
    <tbody>
        <tr>
            <td>0개</td>
            <td>예외 발생</td>
        </tr>
        <tr>
            <td>1개</td>
            <td>주문 성공</td>
        </tr>
        <tr>
            <td>10개</td>
            <td>주문 성공, 재고 0</td>
        </tr>
        <tr>
            <td>11개</td>
            <td>재고 부족 예외</td>
        </tr>
    </tbody>
</table>

경계값 테스트는 일반적인 성공 테스트만으로 발견하기 어려운 오류를 확인하는 데 유용하다.

<br>

### 4.4 테스트 독립성

각 테스트는 다른 테스트의 실행 결과에 의존하지 않아야 한다.

- 테스트마다 필요한 데이터를 독립적으로 준비한다.
- 테스트 실행 순서를 가정하지 않는다.
- 가능하면 하나의 테스트에서 하나의 주요 동작을 검증한다.
- 테스트 간 공유되는 상태를 최소화한다.

<br>
<hr>

## 5. API 테스트

API 테스트는 애플리케이션의 API 엔드포인트에 요청을 보내고, 응답이 예상대로 동작하는지 확인하는 테스트이다.

단위 테스트가 개별 클래스의 기능을 검증한다면, API 테스트는 HTTP 요청부터 응답까지의 동작을 확인한다.

<br>

### 5.1 주요 검증 요소

- HTTP 상태 코드가 올바른지
- 응답 JSON의 구조와 값이 정확한지
- 요청 데이터가 올바르게 처리되었는지
- 잘못된 요청에 대한 적절한 오류 응답을 반환하는지

<br>
<hr>

### 5.2 주요 HTTP 상태 코드

<table>
    <thead>
        <th>상태 코드</th>
        <th>의미</th>
    </thead>
    <tbody>
        <tr>
            <td>200 OK</td>
            <td>요청 처리 성공</td>
        </tr>
        <tr>
            <td>201 Created</td>
            <td>리소스 생성 성공</td>
        </tr>
        <tr>
            <td>400 Bad Request</td>
            <td>잘못된 요청</td>
        </tr>
        <tr>
            <td>401 Unauthorized</td>
            <td>인증 필요 또는 인증 실패</td>
        </tr>
        <tr>
            <td>403 Forbidden</td>
            <td>접근 권한 없음</td>
        </tr>
        <tr>
            <td>404 Not Found</td>
            <td>요청한 리소스를 찾을 수 없음</td>
        </tr>
        <tr>
            <td>500 Internal Server Error</td>
            <td>서버 내부 오류</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

## 6. Postman을 이용한 API 테스트

`Postman`은 HTTP API에 요청을 보내고 응답을 확인할 수 있는 API 테스트 도구이다.

브라우저나 프론트엔드를 구현하지 않아도 API를 직접 테스트할 수 있다.

일반적인 테스트 순서는 다음과 같다.

1. 테스트할 HTTP 메서드와 URL을 입력한다.
2. 필요한 Header를 설정한다.
3. 요청 Body에 JSON 데이터를 작성한다.
4. 요청을 전송한다.
5. 응답 상태 코드와 JSON 데이터를 확인한다.

Postman은 실제 진행 중인 서버를 호출하기 때문에, 애플리케이션과 필요한 외부 시스템이 준비되어야 한다.

<br>
<hr>

## 7. JUnit과 MockMvc를 이용한 API 테스트 자동화

<br>

### 7.1 MockMvc란?

MockMvc는 Spring MVC 애플리케이션의 HTTP 요청과 응답을 실제 서버 실행 없이 테스트할 수 있는 도구이다.

Controller에 가상의 HTTP 요청을 전달하고, 응답 상태 코드나 JSON 결과를 자동으로 검증할 수 있다.

<br>

### 7.2 주요 어노테이션

<table>
    <thead>
        <th>어노테이션</th>
        <th>설명</th>
    </thead>
    <tbody>
        <tr>
            <td>@SpringBootTest</td>
            <td>Spring 애플리케이션 컨텍스트를 로딩</td>
        </tr>
        <tr>
            <td>@AutoConfigureMockMvc</td>
            <td>MockMvc 자동 설정</td>
        </tr>
        <tr>
            <td>@Autowired</td>
            <td>Spring Bean 주입</td>
        </tr>
        <tr>
            <td>@ActiveProfiles("test")</td>
            <td>테스트 전용 설정 적용</td>
        </tr>
        <tr>
            <td>@Transactional</td>
            <td>테스트에서 수행한 DB 작업을 기본적으로 롤백</td>
        </tr>
    </tbody>
</table>

<br>

### 7.3 MockMvc 주요 메서드

- `perform()` : HTTP 요청 실행
- `post(), get()` : 요청 메서드 지정
- `contentType()` : 요청 데이터의 형식 지정
- `content()` : 요청 Body 설정
- `andExpect()` : 응답 결과 검증
- `status().isOk()` : HTTP 200 검증
- `jsonPath()` : JSON 응답의 특정 필드 검증

예를 들어 주문 API에 POST 요청을 보낸 뒤 `HTTP 200`과 `result=true`를 확인할 수 있다.

<br>

### 7.4 @SpringBootTest와 @WebMvcTest의 차이

`@SpringBootTest`는 애플리케이션 컨텍스트를 넓게 로딩해 여러 계층을 연결한 테스트를 수행할 때 사용한다.

반면 `@WebMvcTest`는 주로 Controller와 MVC 관련 컴포넌트를 대상으로 테스트한다.

Service 등의 의존성은 Mock 객체로 대체할 수 있다.

이번 예제에서는 Controller, Service, Repository를 함께 확인하기 위해 `@SpringBootTest`를 사용했다.

<br>

### 7.5 MockMvc와 Postman 비교

<table>
    <thead>
        <th>구분</th>
        <th>Postman</th>
        <th>MockMvc</th>
    </thead>
    <tbody>
        <tr>
            <td>서버 실행</td>
            <td>필요</td>
            <td>불필요</td>
        </tr>
        <tr>
            <td>테스트 방식</td>
            <td>실제 HTTP 요청</td>
            <td>모의 HTTP 요청</td>
        </tr>
        <tr>
            <td>자동화</td>
            <td>컬렉션 및 테스트 스크립트 이용</td>
            <td>Junit과 통합</td>
        </tr>
        <tr>
            <td>활용 목적</td>
            <td>API 수동 확인 및 자동화</td>
            <td>코드 기반 회귀 테스트</td>
        </tr>
    </tbody>
</table>

두 도구는 대체 관계라기보다 상호 보완적으로 활용할 수 있다.

<br>
<hr>

## 8. 정리

<br>

이번 학습을 통해 테스트를 단순히 코드 실행 결과를 확인하는 작업이 아니라, 기능의 요구사항을 명확하게 표현하고 검증하는 과정으로 이해할 수 있었다.

`JUnit 5`는 테스트 실행과 검증의 기본 구조를 제공하고, `Mockito`는 외부 의존성을 대체해 단위 테스트를 독립적으로 수행할 수 있도록 돕는다.

또한 `MockMvc`를 활용하면 HTTP 요청과 응답을 자동으로 검증할 수 있으며, Postman으로 실제 실행 중인 API를 확인할 수 있다.

특히 **성공, 실패, 경계값 테스트를 함께 작성하는 것**이 안정적인 비즈니스 로직을 구현하는 데 중요하다.

<br>

