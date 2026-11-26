<br>

# Week 11 - Day 03. 회고

<br>

## 오늘 배운 것 

- Pod IP가 계속 바뀌는 문제를 Service가 고정 IP와 DNS 이름으로 해결한다는 것을 배웠다.
- `port`, `targetPort`, `nodePort`가 각각 Service, 컨테이너, 노드의 포트라는 것을 정리했다.
- ClusterIP, NodePort, LoadBalancer가 서로를 포함하는 관계이고, 외부 요청이 `LB → NodePort → ClusterIP → Pod` 순서로 들어온다는 것을 배웠다.
- Ingress로 호스트와 경로에 따라 여러 Service에 나눠 보내고, 외부 진입점을 하나로 줄이는 방법을 배웠다.
- Ingress는 규칙일 뿐이고 실제 처리는 Ingress Controller가 한다는 것을 배웠다.
- liveness, readiness, startup Probe가 같은 방식으로 검사하지만 실패했을 때 하는 일이 다르다는 것을 배웠다. 
- 무중단 배포가 readinessProbe, 롤링 업데이트 옵션, preStop과 Graceful Shutdown 세 가지로 완성된다는 것을 배웠다.

<br>

## 헷갈렸던 점

### `port`와 `targetPort`
처음에는 둘이 같은 값이어야 하는 줄 알았다. 정리해보니 `port`는 **클라이언트가 Service에 접속할 때 쓰는 포트**이고, `targetPort`는 **요청이 최종적으로 도착하는 컨테이너 포트**다. 그래서 Service는 80으로 받고 Spring Boot 컨테이너의 8080으로 넘기는 식으로 다르게 둘 수 있다. 다른 서비스는 `order-service:80`만 알면 되고, 컨테이너 포트가 바뀌어도 호출하는 쪽은 고칠 필요가 없다.

### Ingress를 만들었는데 왜 아무 일도 안 일어나나

Ingress도 Service처럼 만들기만 하면 동작하는 줄 알았다. 그런데 Ingress는 **"이렇게 보내라"는 규칙 문서**일 뿐이고, 그 문서를 읽고 실제 nginx 설정을 바꾸는 것은 **Ingress Controller** 다. 컨트롤러가 없거나 `ingressClassName`을 빠뜨리면 규칙은 아무도 읽지 않고, `ADDRESS` 가 비어 있는 채로 남는다. Ingress가 없을 때가 아니라 **규칙을 처리할 주인이 없을 때** 문제가 생긴다는 점을 기억해야겠다.

### `rewrite-target`은 리다이렉트인가
이름만 보고 브라우저를 `/`로 보내는 리다이렉트인 줄 알았다. 실제로는 **컨트롤러가 백엔드로 넘길 때 경로를 바꾸는 것**이고, 사용자의 주소창에는 `/foos1`이 그대로 남는다. 백엔드 nginx는 `/foos1` 페이지가 없으니 `/`로 바꿔 줘야 응답할 수 있다.

### liveness와 readiness의 차이
둘 다 "상태가 괜찮은지" 보는 것이라 차이를 잘 몰랐다. 핵심은 **실패했을 때 무엇을 하느냐**였다. liveness는 컨테이너를 **재시작**하고, readiness는 재시작 없이 **트래픽에서만 뺀다.** 그래서 DB 연결처럼 잠깐 끊길 수 있는 외부 의존성을 liveness에 넣으면, DB가 잠깐 흔들릴 때 모든 앱 컨테이너가 한꺼번에 재시작되는 사고가 난다. 외부 의존성은 readiness에 두어야 한다.

<br>

## 새롭게 알게 된 것

- **ClusterIP는 가상 IP라서 `ping`이 안 될 수 있다.** 장치에 붙은 IP가 아니라 kube-proxy 규칙으로만 존재하기 때문이다. ping이 안 된다고 Service가 고장 난 것은 아니다.
- **NodePort는 Pod가 없는 노드로 들어와도 된다.** kube-proxy가 Pod가 있는 노드로 넘겨준다.
- **LoadBalancer 구현체가 없으면 `EXTERNAL-IP`는 영원히 `<pending>` 이다.** Minikube에서는 `minikube tunnel`을 실행해야 IP가 붙는다.
- **Ingress는 대상 Service가 없어도 만들어진다.** 생성 순서는 상관없지만, 오타가 나도 조용히 만들어지니 `describe`의 Backends를 꼭 확인해야 한다.
- **DNS에 없는 도메인도 `curl -H "Host: ..."`로 테스트할 수 있다.** Ingress는 Host 헤더만 본다.
- **startupProbe가 성공하기 전에는 liveness, readiness가 실행되지 않는다.** liveness의 `initialDelaySeconds` 를 길게 잡으면 실행 중 멈춘 컨테이너를 늦게 발견하는 문제가 있어서 startupProbe가 따로 생겼다.
- **`maxSurge: 1, maxUnavailable: 0`이면 Ready Pod 수가 replicas 아래로 내려가지 않는다.** 대신 잠깐 Pod가 하나 더 떠있을 자원이 필요하다.
- **Pod 종료 시 엔드포인트 제거와 컨테이너 종료가 동시에 시작된다.** 그래서 preStop으로 몇 초 기다려주지 않으면 그 틈에 들어온 요청이 실패한다.
- **PreStop과 Graceful Shutdown 시간은 `terminationGracePeriodSeconds` (기본 30초) 안에 끝나야 한다.** 넘기면 SIGKILL로 강제 종료된다.

<br>

Service는 **고정 주소**, Ingress는 **입구 규칙**, Probe는 **들일지 말지 판단하는 문지기**라는 역할을 나눠 보니, 무중단 배포가 결국 "준비된 Pod에만 요청을 보내고, 떠나는 Pod는 일을 마치고 보내는 것"이라는 한 문장으로 정리되었다.

<br>

