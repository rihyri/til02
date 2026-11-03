<br>

# Week 08 - Day 01. Spring AOP 활용하기

<br>

## 1. AOP란?

<br>

AOP(Aspect-Oriented Programming)는 **관점 지향 프로그래밍**으로, 여러 기능에 반복되는 로깅, 보안, 실행 시간 측정 등의 **횡단 관심사(Cross-Cutting Concern)** 를 핵심 비즈니스 로직과 분리하는 프로그래밍 방식이다.

<br>

### 1.1 AOP가 필요한 이유

AOP를 사용하지 않으면 서비스마다 로그를 남기는 코드가 반복될 수 있다.

```java
public Product findProduct(Long id) {
    log.info("상품 조회 시작");
    Product product = productRepository.findById(id);
    log.info("상품 조회 종료");
    return product;
}
```

- **코드 중복 :** 같은 로깅 코드가 여러 서비스에 흩어진다.
- **유지보수 문제 :** 로그 형식을 바꾸려면 여러 파일을 수정해야 한다.
- **가독성 저하 :** 상품 조회라는 핵심 기능에 부가 기능이 섞인다.

AOP를 적용하면 공통 기능을 Aspect에 모을 수 있다.

```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Before("execution(* com.example.aoppractice.domain..service.*.*(..))")
    public void logBefore() {
        log.info("서비스 메서드 실행 전");
    }
}
```

<br>
<hr>

## 2. Spring AOP 주요 개념

<br>

<table>
    <thead>
        <th>개념</th>
        <th>의미</th>
    </thead>
    <tbody>
        <tr>
            <td>Aspect</td>
            <td>공통 기능을 모아놓은 모듈</td>
        </tr>
        <tr>
            <td>Advice</td>
            <td>실행할 부가 기능과 실행 시점</td>
        </tr>
        <tr>
            <td>JoinPoint</td>
            <td>AOP가 개입할 수 있는 지점. Spring AOP에서는 메서드 실행 지점</td>
        </tr>
        <tr>
            <td>Pointcut</td>
            <td>Advice를 적용할 메서드를 선택할 조건</td>
        </tr>
        <tr>
            <td>Target</td>
            <td>실제 비즈니스 메서드를 가진 객체</td>
        </tr>
        <tr>
            <td>Proxy</td>
            <td>대상 객체 호출을 가로채 부가 기능을 적용하는 객체</td>
        </tr>
    </tbody>
</table>

<br>

### 2.1 Advice 종류

<table>
    <thead>
        <th>Advice</th>
        <th>실행 시점</th>
    </thead>
    <tbody>
        <tr>
            <td>@Before</td>
            <td>대상 메서드 실행 전</td>
        </tr>
        <tr>
            <td>@After</td>
            <td>메서드의 정상/예외 종료 후</td>
        </tr>
        <tr>
            <td>@AfterReturning</td>
            <td>정상 반환 후</td>
        </tr>
        <tr>
            <td>@AfterThrowing</td>
            <td>예외가 전파될 때</td>
        </tr>
        <tr>
            <td>@Around</td>
            <td>실행 전후를 감싸고 진행 여부도 제어</td>
        </tr>
    </tbody>
</table>

<br>

### 2.2 JoinPoint 활용

```java
@Before("execution(* com.example.aoppractice.domain..*.*(..))")
public void logDetails(JoinPoint joinPoint) {
    String methodName = joinPoint.getSignature().getName();
    Object[] args = joinPoint.getArgs();
    log.info("메서드: {}, 인자 수: {}", methodName, args.length);
}
```

- `getSignature().getName()`은 실행되는 메서드 이름을 알려준다.
- `getArgs()`는 메서드에 전달된 인자를 배열로 제공한다.
- **JoinPoint는 실행 지점이고, Pointcut은 실행 지점을 골라내는 조건**이다.

<br>
<hr>

## 3. Pointcut 표현식

<br>

### 3.1 `execution()`

메서드의 반환 타입, 선언 타입, 이름, 파라미터를 기준으로 대상을 고른다.

```java
@Before("execution(* com.example.aoppractice.domain..api.*.*(..))")
```

- 첫 번째 `*` : 반환 타입 무관
- `domain..api` : `domain` 아래의 `api` 패키지
- `*.*`: 해당 패키지의 모든 클래스와 메서드
- `(..)`: 파라미터 개수와 타입 무관

<br>

### 3.2 `within()`

메서드 서명보다 **특정 타입이나 패키지 범위**를 기준으로 적용한다.

```java
@Before("within(com.example.aoppractice.domain..*)")
```

`domain` 및 하위 패키지에 있는 타입을 대상으로 한다.

<br>

### 3.3 `annotation()`

특정 어노테이션을 붙인 메서드만 선택한다.

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable { }
```

```java
@Before("@annotation(com.example.aoppractice.global.annotation.Loggable)")
public void logAnnotated() {
    log.info("@Loggable 메서드 실행 전");
}
```

<table>
    <thead>
        <th>표현식</th>
        <th>기준</th>
        <th>활용 사례</th>
    </thead>
    <tbody>
        <tr>
            <td>execution()</td>
            <td>메서드 실행 조건</td>
            <td>특정 서비스 메서드 로깅</td>
        </tr>
        <tr>
            <td>within()</td>
            <td>클래스·패키지 범위</td>
            <td>계층별 공통 정책</td>
        </tr>
        <tr>
            <td>@annotation()</td>
            <td>어노테이션 존재 여부</td>
            <td>선택적 로깅 또는 시간 측정</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

## 4. 예외 로깅: `@AfterThrowing`

<br>

서비스 계층에서 예외가 발생해 밖으로 전파되면 로그를 남긴다.

```java
@Pointcut("execution(* com.example.aoppractice.domain..service.*.*(..))")
private void serviceMethods() { }

@AfterThrowing(pointcut = "serviceMethods()", throwing = "exception")
public void logException(ProductNotFoundException exception) {
    log.error("상품 조회 오류: {}", exception.getMessage());
}
```

- `@Pointcut`은 적용 조건에 이름을 붙여 재사용하게 한다.
- `throwing`은 발생한 예외를 Adivce 매개변수에 연결한다.
- `ProductNotFoundException` 타입의 예외가 일치할 때 실행된다.
- `@AfterThrowing`은 예외를 **기록**하며, 자동으로 예외를 해결하거나 HTTP 응답을 바꾸지는 않는다.

<br>
<hr>

## 5. 실행 시간 측정: `@Around`

`@Around`는 실행 전후를 모두 감싸므로 소요 시간 측정에 적합하다.

```java
@Around("serviceMethods()")
public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
    long start = System.currentTimeMillis();
    
    try {
        // 실제 대상 메서드 실행
        return joinPoint.proceed();
    } finally {
        long elapsed = System.currentTimeMillis() - start;
        log.info("{} 실행 시간: {}ms", 
                joinPoint.getSignature().toShortString(), elapsed);
    }
}
```

1. 실행 전 시간을 기록한다.
2. `joinPoint.proceed()`로 원래 메서드 실행을 진행한다.
3. 실행 결과를 그대로 반환한다.
4. `finally`에서 정상/예외 종료 모두 소요 시간을 기록한다.

`ProceedingJoinPoint`는 메서드 정보뿐 아니라 `proceed()`로 실행을 진행하는 기능도 제공한다.

`proceed`가 없으면 원래 메서드가 실행되지 않을 수 있다.

<br>
<hr>

## 6. Spring AOP 동작 원리와 주의 사항

<br>

### 6.1 프록시(Proxy)란?

프록시(Proxy)는 **실제 객체를 대신하여 요청을 받아 처리하는 대리 객체**를 의미한다.

클라이언트가 실제 객체를 직접 호출하는 대신 프록시 객체를 호출하면, 프록시는 필요한 부가 기능을 수행한 뒤 실제 객체의 메서드를 실행한다.

예를 들어 `ProductService`에서 상품 조회 기능을 제공한다고 가정하자.

<br>

#### 프록시를 사용하지 않는 경우

```aiignore
Controller → ProductService → 상품 조회
```

Controller가 ProductService를 직접 호출하여 상품 조회 기능을 실행한다.

<br>

#### 프록시를 사용하는 경우

```aiignore
Controller
    ↓
Proxy (대리 객체)
    ├── 실행 전 로깅
    ↓
ProductService (실제 객체)
    └── 상품 조회
    ↓
Proxy
    └── 실행 시간 측정 
    ↓
Controller
```

프록시가 중간에서 요청을 가로채고, 필요한 부가 기능을 수행한 후 결과를 반환한다.

이러한 구조를 **프록시 패턴(Proxy Pattern)**이라고 한다.

<br>

### 6.2 Spring AOP가 프록시를 사용하는 이유

Spring AOP는 핵심 비즈니스 로직을 수정하지 않고도 로깅, 트랜잭션 관리, 실행 시간 측정 등의 공통 기능을 적용하기 위해 프록시를 사용한다.

예를 들어 다음과 같은 서비스가 있다고 가정하자.

```JAVA
@Service
public class ProductService {
    
    public String findProduct() {
        return "상품 조회 완료";
    }
}
```

이 서비스에는 로깅 코드가 존재하지 않는다.

하지만 별도의 Aspect를 작성하면 다음과 같이 공통 기능을 적용할 수 있다.

```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {
    
    @Before("execution(* com.example..ProductService.*(..))")
    public void logBefore() {
        log.info("상품 조회 시작");
    }
}
```

Spring AOP가 적용되는 Bean에 대해 필요한 경우 프록시 객체를 생성한다.

Controller가 `productService`를 호출하면 프록시가 먼저 요청을 받아 Advice를 수행하고, 실제 따라서 **비즈니스 로직에 로깅 코드를 직접 추가하지 않아도 공통 기능을 적용할 수 있다.**

<br>

### 6.3 Spring AOP의 실행 흐름 

Spring AOP는 기본적으로 프록시 기반으로 동작한다.

1. 클라이언트가 Spring Bean의 메서드를 호출한다.
2. 프록시 객체가 요청을 가로챈다.
3. Pointcut 조건과 일치하는지 확인한다.
4. 조건에 일치하는 Advice를 실행한다.
5. 실제 Target 객체의 메서드를 실행한다.
6. 필요한 후처리를 수행한 뒤 결과를 반환한다.

Spring은 일반적으로 JDK 동적 프록시 또는 CGLIB 기반 클래스 프록시를 사용한다.

- **JDK 동적 프록시 :** 주로 인터페이스를 기반으로 프록시를 생성한다.
- **CGLIB 프록시 :** 클래스를 상속하는 방식으로 프록시를 생성한다.

Spring Boot에서는 기본적으로 클래스 기반 프록시를 사용하며, 설정에 따라 변경할 수 있다.

<br>

### 6.4 Spring AOP의 주의사항

Spring AOP는 프록시를 통해 메서드를 호출할 때 Advice를 적용한다.

따라서 **같은 객체 내부에서 다른 메서드를 직접 호출하면 AOP가 적용되지 않을 수 있다.**

이를 자기 호출(Self-Invocation) 문제라고 한다.

```java
@Service
public class ProductService {
    
    public void outer() {
        // 같은 객체 내부에서 메서드 호출
        inner();
    }
    
    @Transactional
    public void inner() {
        // 트랜잭션이 적용되어야 하는 로직
    }
}
```

외부에서 `outer()`를 호출하면 프록시를 거쳐 `outer()`가 실행될 수 있다.

하지만 `outer()` 내부에서 호출한 `inner()`는 프록시를 다시 거치지 않는다.

```aiignore
Controller → Proxy → ProductService.outer() → this.inner() → 프록시를 거치지 않음
```

따라서 `inner()`에 선언한 `@Transactional`이 프록시 기반으로 적용되는 환경이라면, 내부 호출만으로는 해당 트랜잭션 Advice가 실행되지 않는다.

이를 해결하기 위해서는 트랜잭션을 적용할 메서드를 별도의 Spring Bean으로 분리하고, 다른 Bean에서 호출하도록 구성할 수 있다.

또한 여러 Advice가 동일한 메서드에 적용되는 경우에는 `@Order` 등을 사용하여 실행 순서를 명시적으로 관리할 수 있다. 

<br>
<hr>

## 7. 정리

AOP의 핵심은 공통 관심사를 핵심 기능에서 분리하는 것이다.

**Pointcut은 어디에, Advice는 무엇을 언제, Aspect는 이를 모아 둔 클래스**라는 관계를 이해하는 것이 중요하다.

Spring AOP는 프록시 기반이라는 점에서 내부 호출과 적용 범위에도 주의해야 한다.

<br>

