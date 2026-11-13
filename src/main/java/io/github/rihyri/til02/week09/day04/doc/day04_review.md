<br>

# Week 09 - Day 04. 회고

<br>

- Kafka 컨슈머에서 재시도할 예외와 아닌 예외를 나누고, 끝내 실패한 메시지는 DLT로 보내는 방법을 배웠다.
- API 멱등키 설계(누가 만들고, 어디에 저장하고, 중복 요청에 어떻게 응답하는지)와 멱등키와 분산 락의 차이를 정리했다.
- 상태 대신 변경 이력을 쌓는 Event Sourcing의 개념을 배웠다.
- Trace와 Span, Trace ID 전파, Micrometer Tracing + ZipKin 으로 서비스와 Kafka를 넘나드는 요청을 추적하는 방법을 배웠다.
- Micrometer, Actuator, Prometheus, Grafana로 메트릭을 수집하고 대시보드를 만드는 구조를 배웠다.
- OpenTelemetry Collector를 앞에 두어 수집 경로를 표준화하는 방법을 배웠다.

<br>

### 1. 누가 무엇을 하는지 경계가 흐려서 어려웠다.
Micrometer, Actuator, Prometheus, Grafana가 한꺼번에 나오니까 메트릭이 어디서 만들어지고 어디에 저장되는지 섞였다. 특히 Grafana가 데이터를 저장하는 줄 알았다.

정리한 결론은 "앱은 진열만, Prometheus는 가져가서 저장, Grafana는 물어보고 그리기만" 이다. `/actuator/prometheus`에는 지금 값만 있고, 시간에 따른 그래프가 그려지는 것은 Prometheus가 주기적으로 가져가 쌓기 때문이라는 것도 이때 이해했다.

### 2. 코드의 이름과 Grafana의 이름이 달랐다.
코드에서는 `http.server.requests` 인데 쿼리에서는 `http_server_requests_seconds_count`라서 같은 것인지 확신하지 못했다. Micrometer가 Prometheus로 내보낼 때 **점을 밑줄로 바꾸고, 단위와 접미사를 붙이고, Timer를 count, sum, bucket으로 쪼깬다**는 것을 공식 문서에서 확인하고 나서야 연결됐다.

덤으로 Grafana 데이터소스에 `localhost:9090`을 넣으면 안 되는 이유도 이해했다. Grafana 컨테이너 안에서 localhost는 Grafana 자신이다. ZipKin 주소, Prometheus 수집 대상 주소가 전부 서비스 이름으로 되어 있던 것도 같은 이유다.

<br>

- **Spring Boot의 기본 샘플링 비율은 10%다.** 실습에서는 1.0으로 바꿨지만, 설정을 빼먹으면 Zipkin에 요청이 일부만 보여서 오류인 줄 착각할 수 있다.
- **Kafka 구간은 설정을 켜야 Trace가 이어진다.** `observation-enabled` 기본값이 false라서, 켜지 않으면 HTTP까지만 보이고 Kafka에서 끊긴다.
- **Summary보다 Histogram이 MSA에 유리하다.** Summary는 인스턴스마다 미리 계산된 백분위라 합칠 수 없고, Histogram은 버킷을 더한 뒤 계산할 수 있다.
- **비즈니스 메트릭도 커밋 후에 세야 한다.** 주문 수 Counter를 `@TransactionEventListener` 에서 올려서 롤백된 주문은 세지 않는다.
- **OTel Controller를 써도 코드는 그대로다.** 의존성(Registry)만 바꿨는데 같은 PromQL이 그대로 동작했다. 대신 Collector가 새로운 장애 지점이 되므로 Collector 자신도 감시해야 한다.

<br>

Prometheus와 Grafana가 헷가렸던 것은 개념이 어려워서가 아니라 역할의 경계와 이름 변환을 몰랐기 때문이다. **"앱은 진열, Prometheus는 저장, Grafana는 그림",** 그리고 **"메트릭 타입이 쿼리를 정한다."** 두 문장으로 정리됐다.

<br>

