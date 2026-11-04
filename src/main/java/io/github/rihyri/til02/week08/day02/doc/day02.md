<br>

# Week 08 - Day 02. 리팩토링과 클린 코드

<br>

## 1. `리팩토링(Refactoring)`이란?

<br>

리팩토링은 **외부에서 관찰되는 동작은 유지하면서 구조를 개선**하는 작업이다. 기능을 새로 만드는 것과 달리, 기존 동작을 유지하는 것이 핵심이다.

- **목적 :** 가독성, 유지보수성, 확장성 개선
- **방법 :** 중복 제거, 메서드 추출, 책임 분리, 명확한 네이밍
- **주의 :** 한 번에 크게 바꾸기보다 작게 변경하고 테스트로 동작을 확인한다.

<br>
<hr>

## 2. 코드 스멜(Code smell)

<br>

코드 스멜은 코드를 개선할 필요가 있음을 알려주는 **징후**이다. 발견했다고 해서 반드시 버그라는 뜻은 아니다.

<table>
    <thead>
        <th>코드 스멜</th>
        <th>문제점</th>
        <th>개선 방법</th>
    </thead>
    <tbody>
        <tr>
            <td>중복 코드</td>
            <td>동일한 수정이 여러 곳에 필요</td>
            <td>메서드 추출 (Extract Method)</td>
        </tr>
        <tr>
            <td>긴 메서드</td>
            <td>기능과 흐름 파악이 어려움</td>
            <td>책임에 따라 메서드 분리</td>
        </tr>
        <tr>
            <td>과도한 책임</td>
            <td>하나의 클래스가 여러 이유로 변경</td>
            <td>클래스 분리, SRP 적용</td>
        </tr>
        <tr>
            <td>모호한 이름</td>
            <td>변수나 메서드의 의도를 추측해야 함</td>
            <td>이름 변경 (Rename)</td>
        </tr>
        <tr>
            <td>무분별한 Setter</td>
            <td>상태 변경 규칙을 우회하기 쉬움</td>
            <td>의미 있는 도메인 메서드 제공</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

## 3. 리팩토링 대표 기법

<br>

### 3.1 메서드 추출 (Extract Method)

반복되거나 독립된 의미를 가진 코드 블록을 별도 메서드로 분리한다. 단순히 코드를 짧게 만드는 것이 아니라 **이름으로 의도를 드러내는 것**도 중요하다.

```java
public void validateOrderStatus(OrderStatus status) {
    if (status != OrderStatus.PENDING) {
        throw new IllegalStateException("취소 가능한 주문이 아닙니다.");
    }
}
```

이 메서드는 별도 학습용 예시이며, 예시 코드에서는 `Order.cancelOrder()` 안에 상태 검증을 두어 도메인 규칙이 흩어지지 않도록 하였다.

<br>

### 3.2 네이밍 개선 (Rename)
`check()`보다 `validateOrderStatus()`가 구체적이고 목적을 명확하게 나타낸다. 클래스는 역할을 표현하는 명사, 메서드는 수행하는 행위를 표현하는 이름을 사용하는 것이 좋다.

<br>

### 3.3 역할 분리
하나의 Service에 조회, 상태 검증, 변경 규칙이 모두 들어 있다면, 데이터 자신의 규칙은 Entity로 옮길 수 있다. 다만 모든 로직을 Entity로 옮겨야 한다는 뜻은 아니다.

<br>
<hr>

## 4. 빈약한 도메인 모델과 풍부한 도메인 모델

<br>

### 4.1 빈약한 도메인 모델 (Anemic Domain Model)

Entity는 상태 및 Getter/Setter를 주로 보유하고, Service에서 상태 검증과 변경을 수행한다.

```java
// Order.java : 상태 변경을 외부에 허용
@Setter
private OrderStatus status;

// OrderService.java : 서비스가 규칙을 직접 처리
if (order.getStatus() != OrderStatus.PENDING) {
    throw new OrderException("취소할 수 없는 주문입니다.");    
}
order.setStatus(OrderStatus.CANCELED);
```

단점은 상태 변경 규칙이 여러 서비스에 중복될 수 있고, 다른 곳에서 Setter를 직접 호출해 규칙을 우회할 수 있다는 것이다.

<br>

### 4.2 풍부한 도메인 모델 (Rich Domain Model)

Entity가 자신의 상태와 연관된 비즈니스 규칙을 메서드로 관리한다.

```java
// Order.java 
public void cancelOrder() {
    
    if (this.status != OrderStatus.PENDING) {
        throw new OrderException("취소할 수 없는 주문입니다.");
    }
    this.status = OrderStatus.CANCELED;
}

// OrderService.java 
@Transactional
public void cancel(Long orderId) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException("존재하지 않는 주문입니다."));
    order.cancelOrder();
}
```

**변화 :** Service는 주문을 다시 조회하고 동작을 요청하며, Entity는 취소할 수 있는지 판단하고 상태를 변경한다. 따라서 상태와 행위가 함께 있어 응집도가 높아진다.

<br>

<table>
    <thead>
        <th>비교</th>
        <th>빈약한 모델</th>
        <th>풍부한 모델</th>
    </thead>
    <tbody>
        <tr>
            <td>상태 검증</td>
            <td>주로 Service</td>
            <td>Entity 내부</td>
        </tr>
        <tr>
            <td>상태 변경</td>
            <td>Setter 등으로 외부 변경</td>
            <td>의미 있는 메서드를 통해 변경</td>
        </tr>
        <tr>
            <td>Service의 역할</td>
            <td>업무 규칙까지 직접 처리</td>
            <td>흐름 조정과 협력</td>
        </tr>
        <tr>
            <td>유의점</td>
            <td>규칙 분산 가능성</td>
            <td>Entity의 책임이 과도해지지 않도록 주의</td>
        </tr>
    </tbody>
</table>

> 두 모델은 항상 어느 하나가 정답인 관계는 아니다. 도메인 규칙의 복잡도와 프로젝트 특성에 따라 선택해야 한다.

<br>

### 4.3 @Transactional과 변경 감지

JPA가 관리하는 Entity를 트랜잭션 안에서 조회한 뒤 필드를 수정하면, JPA 구현체의 변경 감지(dirty checking)가 이를 포착하여 데이터베이스에 반영할 수 있다. 따라서 위 `cancel()` 예제에는 `orderRepository.save(order)`를 별도로 호출하지 않는다. Spring Data JPA 공식 문서는 Repository 추상화의 일관성을 위해 `save()`를 호출할 수도 있다고 안내한다.

<br>
<hr>

## 5. 클린 코드(Clean Code)의 핵심 원칙

<br>

### 5.1 명확한 네이밍

이름만으로 무엇을 저장하고 수행하는지 알 수 있어야 한다. 같은 개념에는 같은 용어를 사용하고, 불필요한 약어는 피한다.

### 5.2 DRY (Don't Repeat Yourself)

중복된 지식과 규칙을 여러 위치에서 따로 관리하지 않도록 한다. 단, **모양만 비슷한 코드**까지 무조건 묶으면 오히려 결합도가 증가할 수 있다.

### 5.3 SRP (Single Responsibility Principle)

클래스는 변경되어야 하는 이유가 하나가 되도록 책임을 구성한다. 메서드도 하나의 명확한 역할에 집중하도록 만든다.

### 5.4 읽기 쉬운 구조

일관된 들여쓰기와 공백, 적절한 줄바꿈을 사용하고, 긴 메서드를 의미 단위로 나눈다. 지나치게 잘게 쪼개거나 불필요하게 추상화하는 것은 피한다.

<br>
<hr>

## 6. 좋은 주석과 나쁜 주석

<br>

- **피할 주석 :** 코드 내용을 그대로 읽어 주는 주석, 이미 틀린 설명, 불필요한 의무적 주석
- **도움이 되는 주석 :** 코드만으로 알기 어려운 설계 의도, 정책의 근거, 주의할 부작용, 구체적인 TODO

```java
// 나쁜 예 : 상태를 취소로 바꾼다.
this.status = OrderStatus.CANCELED;

// 좋은 예 : 외부 결제 취소가 선행되어야 한다는 정책상 제약 설명
// 결제 승인 취소가 완료된 주문에 대해서만 호출한다.
```

두 번째 주석은 **정말로 해당 정책과 호출 순서가 존재할 때만** 작성해야 한다. 사실과 다른 주석은 주석이 없는 것보다 위험하다.

<br>
<hr>

## 7. 정리

<br>

- 리팩토링은 **동작을 유지하면서 구조를 개선**하는 작업이다.
- 코드 스멜을 발견하면 메서드 추출, 이름 개선, 책임 분리 등을 검토한다.
- 주문 취소 예제처럼 **상태와 관련된 규칙을 Entity에 모으면,** 다른 코드가 규칙을 우회할 가능성을 줄일 수 있다.
- 클린 코드의 목표는 무조건 짧은 코드가 아니라 **읽기 쉽고 안전하게 변경할 수 있는 코드**이다.
- 테스트를 통해 리팩토링 전후의 기능이 유지되는지 확인하는 습관이 중요하다.

<br>

