<br>

# Week 11 - Day 03. 

<br>

이번 수업은 **Pod 앞에 고정 주소를 두는 Service → 여러 Service를 하나의 입구로 묶는 Ingress → 컨테이너 상태를 검사하는 Probe → 이 셋을 엮어 만드는 무중단 배포** 순서로 진행됐다. 그 중 **Service 세 가지 타입의 포함 관계와 liveness, readiness, startup Probe가 실패했을 때 각각 하는 일**이 특히 중요하다.

<br>
<hr>

## 1. Service 

<br>

### 1.1 Service가 필요한 이유

Pod는 지워지고 다시 생길 때마다 IP가 바뀐다. 그래서 다른 서비스가 Pod IP를 직접 들고 있으면 금방 연결이 끊긴다. Service는 **바뀌는 Pod들 앞에 바뀌지 않는 IP와 DNS 이름을 하나 세우고,** 들어온 요청을 뒤의 Pod들에 나눠 준다. 직원이 바뀌어도 그대로인 회사 대표번호와 같다.

<table>
    <thead>
        <tr>
			<th>구성 요소</th>
			<th>하는 일</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>라벨 셀렉터</td>
            <td>트래픽을 받을 Pod를 고른다. Deployment가 Pod를 찾는 방식과 같다.</td>
        </tr>
        <tr>
            <td>EndpointSlice</td>
            <td>셀렉터에 맞는 Pod IP 목록. Pod가 생기거나 사라지면 자동으로 바뀐다.</td>
        </tr>
        <tr>
            <td>kube-proxy</td>
            <td>각 노드에서 iptables 같은 규칙을 만들어 실제 요청을 Pod로 보낸다.</td>
        </tr>
    </tbody>
</table>

ClusterIP는 어떤 네트워크 장치에도 붙어 있지 않은 **가상 IP**다. 그래서 `ping`은 안 될 수 있지만, Service에 정해 둔 포트로 보낸 요청은 정상적으로 전달된다.

```aiignore
# Service 목록과 CLUSTER-IP, PORT(S)
kubectl get svc
# 실제로 연결된 Pod IP
kubectl get endpointslices -l kubernetes.io/service-name=my-svc
# Selector, Endpoints 확인
kubectl describe svc my-svc
```

<br>

### 1.2 포트 세 가지

<table>
    <thead>
        <tr>
			<th>필드</th>
			<th>누구의 포트인가</th>
			<th>메모</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>port</td>
            <td>Service</td>
            <td>클라이언트는 서비스이름:port로 접속한다.</td>
        </tr>
        <tr>
            <td>targetPort</td>
            <td>Pod (컨테이너)</td>
            <td>요청이 최종적으로 도착하는 포트. 생략하면 port와 같다.</td>
        </tr>
        <tr>
            <td>nodePort</td>
            <td>각 노드</td>
            <td>NodePort 타입일 때만. 30000~32767 범위, 생략하면 자동 할당</td>
        </tr>
    </tbody>
</table>

```yaml
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  type: NodePort
  selector:
    add: order            # 이 라벨을 가진 Pod 로 보낸다.
  ports:
    - port: 80            # 클러스터 안에서 order-service:80
      targetPort: 8080    # 컨테이너는 8080 에서 받는다.
      nodePort: 30080     # 밖에서는 노드 IP:30080
```

<br>

### 1.3 서비스 디스커버리 (DNS)

클러스터 DNS(CoreDNS)가 Service 마다 이름을 자동으로 등록한다. 설정 파일에 IP 대신 **서비스 이름**을 적어 두면, 뒤의 Pod가 몇 번 바뀌어도 설정을 고칠 일이 없다.

```aiignore
<서비스 이름>.<네임스페이스>.svc.cluster.local
```

```bash
curl http://order-service                         # 같은 네임 스페이스
curl http://order-service.shop                    # shop 네임 스페이스의 Service
curl http://order-service.shop.svc.cluster.local  # 전체 이름

# 임시 Pod 를 띄워 클러스터 안에서 확인
kubectl run tmp --rm --it --image=busybox --restart=Never -- wget -qo- http://order-service
```

<br>

### 1.4 Service 타입 ⭐

세 타입은 **위로 갈수록 아래 타입을 포함**한다. NodePort를 만들면 ClusterIP가 함께 생기고, LoadBalancer를 만들면 NodePort와 ClusterIP가 함께 생긴다. 외부 요청은 `외부 로드밸런서 → 노드 포트 → ClusterIP → Pod` 순서로 들어온다.

<table>
    <thead>
        <tr>
			<th>타입</th>
			<th>접근 범위</th>
			<th>접속 주소</th>
			<th>주 용도</th>
            <th>단점</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>ClusterIP (기본)</td>
            <td>클러스터 내부만</td>
            <td>서비스이름:port</td>
            <td>서비스 간 내부 통신, DB, 내부 API</td>
            <td>밖에서 접근 불가</td>
        </tr>
        <tr>
            <td>NodePort</td>
            <td>노드 IP를 아는 외부</td>
            <td>노드 IP : 30000~32767</td>
            <td>개발, 테스트, 온프레미스, 외부 LB 연동</td>
            <td>포트 범위 제한, 노드 IP가 바뀌거나 노드가 죽으면 주소를 바꿔야 함</td>
        </tr>
        <tr>
            <td>LoadBalancer</td>
            <td>인터넷</td>
            <td>EXTERNAL-IP:port</td>
            <td>운영 환경의 외부 공개</td>
            <td>Service마다 로드밸런서가 하나씩 생겨 비용 증가</td>
        </tr>
    </tbody>
</table>

- `type`을 적지 않으면 ClusterIP가 된다.
- NodePort는 **모든 노드에 같은 포트**를 연다. Pod가 노드 1에만 있어도 노드 2의 포트로 들어온 요청을 kube-proxy가 노드 1로 넘겨준다.
- LoadBalancer는 클라우드나 MetalLB처럼 **로드밸런서 구현체가 있어야** 동작한다. 없는 환경(Minikube 기본 상태)에서는 `EXTERNAL-IP`가 계속 `<pending>`으로 남는다.

```bash
kubectl expose deployment my-app --type=NodePort --port=80 --target-port=80
# Minikube 에서 NodePort 접속 URL 받기
minikube service my-app --url
# Minikube 에서 LoadBalancer 에 EXTERNAL-IP 붙이기 (터미널 유지)
minikube tunnel
```

<br>
<hr>

## 2. Ingress

<br>

### 2.1 Ingress가 하는 일

외부 요청은 **Ingress(입구 규칙) → Service(고정 주소) → Pod(실제 앱)** 순서로 들어온다. Ingress는 HTTP/HTTPS 요청의 **호스트 이름과 URL 경로**를 보고 어느 Service로 보낼지 정한다.

Service마다 LoadBalancer를 만들면 그 수만큼 비용이 들지만, Ingress를 쓰면 **외부 진입점 하나로 여러 Service를 노출**할 수 있다.

<table>
    <thead>
        <tr>
			<th>기능</th>
			<th>예</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>경로 기반 라우팅</td>
            <td>/order → order-service, /member → member-service</td>
        </tr>
        <tr>
            <td>호스트 기반 라우팅(가상 호스팅)</td>
            <td>shop.example.com, admin.example.com을 다른 Service로</td>
        </tr>
        <tr>
            <td>TLS 종료</td>
            <td>HTTPS 인증서는 Ingress에서 처리하고 안쪽은 HTTP로 전달</td>
        </tr>
    </tbody>
</table>

<br>

### 2.2 Ingress와 Ingress Controller ⭐

**Ingress는 규칙을 적어 둔 설정일 뿐이고 스스로는 아무것도 하지 않는다.** 규칙을 읽어 실제로 트래픽을 받고 넘겨주는 것은 클러스터 안에 Pod로 떠 있는 **Ingress Controller** (예: ingress-nginx)다. 컨트롤러가 없으면 Ingress를 만들어도 요청은 어디로 가지 않는다.

컨트롤러는 Ingress 리소스를 계속 지켜보다가 규칙이 바뀌면 자기 설정(nginx.config 등)을 자동으로 고친다. 요청 흐름은 이렇다.

```aiignore
클라이언트 → 컨트롤러 Service(NodePort/LB) → 컨트롤러 Pod(nginx)
           → 규칙에 맞는 백엔드 Service → 백엔드 Pod
```

<br>

### 2.3 Ingress Controller 설치

두 방법을 같이 쓰면 충돌하므로 **하나만** 고른다.

<table>
    <thead>
        <tr>
			<th>방법</th>
			<th>대상</th>
			<th>명령</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>A. Minikube 애드온 (권장)</td>
            <td>Minikube</td>
            <td>minikube addons enable ingress</td>
        </tr>
        <tr>
            <td>B. 매니페스트 직접 적용</td>
            <td>kind, kubeadm 등 애드온이 없는 클러스터</td>
            <td>ingress-nginx 공식 deploy.yaml 적용</td>
        </tr>
    </tbody>
</table>

방법 B의 실습 파일은 원본 매니페스트에서 **컨트롤러 Service의 `type: LoadBalancer`를 `NodePort`로 바꾼** 버전이다. 로드 밸런서가 없는 환경에서도 노드 포트로 컨트롤러에 들어갈 수 있게 하기 위해서다.

매니페스트에 들어 있는 주요 리소스는 다음과 같다.

<table>
    <thead>
        <tr>
			<th>리소스</th>
			<th>역할</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>Namespace ingress-nginx</td>
            <td>컨트롤러 전용 공간</td>
        </tr>
        <tr>
            <td>ServiceAccount, (Cluster)Role, (Cluster)RoleBinding</td>
            <td>컨트롤러가 Ingress, Service, EndPointSlice를 조회할 권한 (RBAC)</td>
        </tr>
        <tr>
            <td>ConfigMap</td>
            <td>nginx 전역 설정</td>
        </tr>
        <tr>
            <td>Service (NodePort)</td>
            <td>외부 트래픽이 컨트롤러로 들어오는 입구 (80, 443)</td>
        </tr>
        <tr>
            <td>Deployment</td>
            <td>실제 nginx 컨트롤러 Pod</td>
        </tr>
        <tr>
            <td>Job, ValidatingWebhookConfiguration</td>
            <td>잘못된 ingress를 생성 시점에 거부하는 검증 웹훅과 그 인증서</td>
        </tr>
        <tr>
            <td>IngressClass nginx</td>
            <td>ingressClassName: nginx로 이 컨트롤러를 지정할 수 있게 함</td>
        </tr>
    </tbody>
</table>

```bash
minikube addons enable ingress                        # 방법 A
kubectl apply -f week-11/day-03/ingress/deploy.yaml   # 방법 B

# controller Pod 가 Running 인지
kubectl get pods -n ingress-nginx

# 컨트롤러 Service 의 NodePort 확인
kubectl get svc -n ingress-nginx

# nginx IngressClass 생성 확인
kubectl get ingressClass
```

<br>

### 2.4 Ingress 규칙 읽기

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: test
  annotations:
    # 백엔드로 넘길 때 경로를 / 로 바꿈
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
ingressClassName: nginx   # 이 규칙을 처리할 컨트롤러
rules:
  - host: foo.bar.com
    http:
      paths:
        - path: /foos1    # foo.bar.com/foos1 → s1:80
          pathType: Prefix
          backend:
            service:
              name: s1
              port:
                number: 80
        - path: /bars2    # foo.bar.com/bars2 → s2:80
          pathType: Prefix
          backend:
            service:
              name: s2
              port:
                number: 80
  - host: bar.foo.com     # bar.foo.com / → s2:80
    http:
      paths:
        - path: /
          pathType: Prefix
          backend:
            service:
              name: s2
              port:
                number: 80
```

<table>
    <thead>
        <tr>
			<th>항목</th>
			<th>의미</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>host</td>
            <td>요청의 Host 헤더로 구분한다. 같은 IP로 들어와도 도메인마다 다른 Service로 보낼 수 있다.</td>
        </tr>
        <tr>
            <td>pathType: Prefix</td>
            <td>경로가 그 값으로 시작하면 일치 (/foos1, /foos1/abc)</td>
        </tr>
        <tr>
            <td>pathType: Exact</td>
            <td>경로가 정확히 같을 때만 일치</td>
        </tr>
        <tr>
            <td>rewrite-target</td>
            <td>리다이렉트가 아니다. 브라우저 주소는 /foos1 그대로고, 컨트롤러가 백엔드로 넘길 때만 경로를 /로 바꾼다. 백엔드 nginx에는 /foos1 페이지가 없기 때문이다.</td>
        </tr>
    </tbody>
</table>

DNS에 등록하지 않은 도메인도 `Host` 헤더에 넣어 주면 Ingress는 그 도메인 요청으로 처리한다.

```bash
kubectl apply -f ingress.yaml
kubectl get ingress                 # ADDRESS 가 채워졌는지
kubectl describe ingress test       # Rules, Backends 확인

NODE_IP=$(minikube ip)
PORT=$(kubectl get svc ingress-nginx-controller -n ingress-nginx \
  -o jsonpath='{.spec.ports[?(@.name=="http")].nodePort}')
curl -H "Host: foo.bar.com" http://$NODE_IP:$PORT/foos1     # s1 응답
curl -H "Host: bar.foo.com" http://$NODE_IP:$PORT/          # s2 응답 
```

<br>

### 2.5 주의할 점

- **`ingressClassName`을 빠뜨리면,** 기본 IngressClass가 없는 클러스터에서는 어떤 컨트롤러도 규칙을 처리하지 않는다. 이때 `kubectl get ingress`의 `ADDRESS`가 비어 있다.
- Ingress는 **만들 때 대상 Service가 있는지 검사하지 않는다.** 그래서 Service보다 먼저 만들어도 된다. 대상이 없으면 `describe`의 Backends에 `services "s1" not found`가 보인다.

<br>
<hr>

## 3. Probe - Kubelet의 컨테이너 상태 검사 ⭐

<br>

### 3.1 Probe가 필요한 이유

프로세스가 떠 있다고 애플리케이션이 정상이라는 보장이 없다. 데드락에 걸렸을 수도, 아직 초기화 중일 수도 있다. 그래서 kubelet은 애플리케이션에 **직접 상태를 물어보는 Probe**를 주기적으로 실행하고, 결과에 따라 컨테이너를 재시작하거나 트래픽을 막는다.

<br>

### 3.2 검사 방식

<table>
    <thead>
        <tr>
			<th>방식</th>
			<th>성공 조건</th>
			<th>예</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>httpGet</td>
            <td>응답 코드가 200 이상 400 미만</td>
            <td>GET /actuator/health</td>
        </tr>
        <tr>
            <td>tcpSocket</td>
            <td>지정 포트에 TCP 연결 성공</td>
            <td>DB, Redis 포트</td>
        </tr>
        <tr>
            <td>exec</td>
            <td>컨테이너 안에서 실행한 명령의 종료 코드가 0</td>
            <td>mysqladmin ping</td>
        </tr>
        <tr>
            <td>grpc</td>
            <td>gRPC 헬스 체크 응답이 SERVING</td>
            <td>gRPC 서버</td>
        </tr>
    </tbody>
</table>

<br>

### 3.3 세 가지 Probe

세 Probe는 검사 방식은 같고 **실패했을 때 하는 일**이 다르다.

<table>
    <thead>
        <tr>
			<th>Probe</th>
			<th>묻는 것</th>
			<th>실패하면</th>
			<th>설정하지 않으면</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>livenessProbe</td>
            <td>살아 있나?</td>
            <td>컨테이너 재시작</td>
            <td>프로세스가 살아 있는 한 성공으로 본다.</td>
        </tr>
        <tr>
            <td>readinessProbe</td>
            <td>요청을 받을 준비가 됐나?</td>
            <td>재시작하지 않고 Service 엔드포인트에서 제외</td>
            <td>바로 Ready로 본다.</td>
        </tr>
        <tr>
            <td>startupProbe</td>
            <td>시작을 마쳤나?</td>
            <td>failureThreshold 를 넘으면 컨테이너 재시작</td>
            <td>바로 시작 완료로 본다.</td>
        </tr>
    </tbody>
</table>

```aiignore
컨테이너 시작 → startupProbe ──계속 실패──→ 컨테이너 재시작
                   │ 성공
                   ▼
        liveness, readiness 검사 시작
          ├─ liveness 실패  → 컨테이너 재시작
          └─ readiness 실패 → 트래픽에서 제외 (성공하면 다시 포함)
```

식당으로 비교하면 liveness는 "가게가 망했는지", readiness는 "지금 손님을 받을 수 있는지"를 본다. 재료를 준비 중인 가게는 문만 잠깐 닫으면 되지 가게를 새로 차릴 필요는 없다.

<br>

#### liveness를 설정할 때
- 실패 판정을 너무 빠르게 잡으면, 트래픽이 몰려 잠깐 느려진 컨테이너까지 재시작된다. 그러면 남은 컨테이너로 부하가 몰려 **연쇄 재시작**이 일어난다. `timeoutSeconds`와 `failureThreshold`를 넉넉하게 둔다.
- **DB 같은 외부 의존성은 넣지 않는다.** DB가 잠깐 끊기면 모든 애플리케이션 컨테이너가 동시에 재시작되기 때문이다. 외부 의존성 확인은 readiness 쪽에 둔다.

#### readiness
- 롤링 업데이트 중 **아직 준비되지 않은 새 Pod로 요청이 가는 것을 막는** 무중단 배포의 핵심이다.
- 준비 여부는 `kubectl get pods`의 `READY` 열 `(0/1, 1/1)`로 본다.

#### startup

- SpringBoot처럼 기동이 오래 걸리는 앱에서, 시작 도중 liveness가 실패해 **뜨지도 못하고 재시작만 반복하는** 문제를 막는다.
- 예전에는 liveness의 `initialDelaySeconds`를 길게 잡아 해결했지만, 그러면 실행 중에 멈춘 컨테이너도 그만큼 늦게 발견된다. startupProbe는 **시작 단계와 실행 단계의 기준을 분리**한다.

<br>

### 3.4 설정 예시와 옵션

```yaml
containers:
  - name: app
    image:memo-app: 1.0.0
    ports:
      - containerPort: 8080
    startupProbe:
      httpGet:
        path: /actuator/health
        port: 8080
      periodSeconds: 5
      failureThreshold: 30
    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 8080
      periodSeconds: 5
      failureThreshold: 3
    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
      periodSeconds: 10
      failureThreshold: 3
```

<table>
    <thead>
        <tr>
			<th>옵션</th>
			<th>기본값</th>
			<th>의미</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>initialDelaySeconds</td>
            <td>0</td>
            <td>컨테이너 시작 후 첫 검사까지 기다리는 시간</td>
        </tr>
        <tr>
            <td>periodSeconds</td>
            <td>10</td>
            <td>검사 주기</td>
        </tr>
        <tr>
            <td>timeoutSeconds</td>
            <td>1</td>
            <td>응답을 기다리는 시간</td>
        </tr>
        <tr>
            <td>failureThreshold</td>
            <td>3</td>
            <td>연속 몇 번 실패해야 실패로 볼지</td>
        </tr>
        <tr>
            <td>successThreshold</td>
            <td>1</td>
            <td>실패 후 연속 몇 번 성공해야 다시 성공으로 볼지 (liveness, startup은 1만 가능)</td>
        </tr>
    </tbody>
</table>

Spring Boot는 Kubernetes 환경에서 `/actuator/health/liveness`, `/actuator/health/readiness` 그룹을 자동으로 켠다. 로컬에서 확인하려면 다음 설정에서 직접 켤 수 있다.

```aiignore
management.endpoint.health.probes.enabled=true
```

```bash
# READY 0/1 → 1/1 변화 지켜보기
kubectl get pods -w
# Events 에 Liveness/Readiness probe failed 가 찍힘
kubectl describe pod <pod>
# readiness 실패 Pod 가 빠졌는지
kubectl get endpointslices -l kubernetes.io/service-name=my-svc
```

<br>
<hr>

## 4. 무중단 배포 ⭐

<br>

### 4.1 세 가지가 맞물려야 한다.

롤링 업데이트는 **새 버전 Pod를 조금씩 늘리면서 이전 버전 Pod를 조금씩 줄이는** 방식이다. 달리는 버스의 바퀴를 하나씩 갈아 끼우는 것과 같다. 다만 롤링 업데이트만으로는 부족하고, 다음 세 가지가 함께 있어야 실제로 요청이 끊기지 않는다.

<table>
    <thead>
        <tr>
			<th>시점</th>
			<th>지켜주는 것</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>새 Pod를 들일 때</td>
            <td>readinessProbe - 준비되기 전에는 트래픽을 받지 않음</td>
        </tr>
        <tr>
            <td>교체 속도와 순서</td>
            <td>롤링 업데이트 옵션 - maxSurge, maxUnavailable</td>
        </tr>
        <tr>
            <td>이전 Pod를 내보낼 때</td>
            <td>PreStop + Graceful Shutdown - 트래픽이 끊긴 뒤, 처리 중인 요청을 마치고 종료</td>
        </tr>
    </tbody>
</table>

<br>

### 4.2 배포 설정

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
  annotations:
    kubernetes.io/change-cause: "nginx 1.26 최초 배포"   # rollout history 에 표시
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1          # replicas 보다 1개까지 더 만들 수 있음 (최대 4개)
      maxUnavailable: 0    # Ready Pod 가 3개 아래로 내려가지 않음
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
        - name: my-container
          image: nginx:1.26
          ports:
            - containerPort: 80
          readinessProbe:
            httpGet:
              path: /
              port: 80
            initialDelaySeconds: 5
            periodSeconds: 5
          livenessProbe:
            httpGet:
              path: /
              port: 80
            initialDelaySeconds: 15
            periodSeconds: 20
          lifecycle:
            preStop:
              exec:
                command: ["sleep", "5"]   # 종료 전 5초 대기
```

<table>
    <thead>
        <tr>
			<th>옵션</th>
			<th>의미</th>
			<th>이 예제 (replicas 3)</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>type: RollingUpdate</td>
            <td>조금씩 교체 (기본값)</td>
            <td>사용</td>
        </tr>
        <tr>
            <td>type: Recreate</td>
            <td>기존 Pod를 모두 내린 뒤 새로 만듦. 중단이 생긴다. 두 버전이 동시에 뜨면 안될 때(DB 스키마 변경 등) 쓴다.</td>
            <td>사용 안 함</td>
        </tr>
        <tr>
            <td>maxSurge</td>
            <td>업데이트 중 replicas보다 추가로 만들 수 있는 수 (개수 또는 %, 기본 25%)</td>
            <td>최대 4개</td>
        </tr>
        <tr>
            <td>maxUnavailable</td>
            <td>업데이트 중 준비되지 않아도 되는 수 (개수 또는 %, 기본 25%)</td>
            <td>항상 3개 Ready</td>
        </tr>
    </tbody>
</table>

진행 순서는 이렇다. 새 v2 Pod 하나를 만들고 → readiness를 통과해 Ready가 되면 → v1 Pod 하나를 종료한다. 이것을 반복해 모두 v2가 되면 끝난다. 어느 순간에도 Ready Pod는 3개 아래로 떨어지지 않는다.

**두 버전이 동시에 트래픽을 받는 구간이 있다.** 그래서 롤링 업데이트를 하려면 API와 DB 스키마가 이전 버전과 **하위 호환**되어야 한다.

<br>

### 4.3 이전 Pod를 안전하게 내보내기 (preStop, Graceful Shutdown)

Pod가 종료 상태가 되면 **엔드포인트에서 빠지는 일과 컨테이너 종료**가 동시에 시작된다. 엔드포인트 변경이 각 노드의 kube-proxy에 반영되기 전에 컨테이너가 먼저 내려가면, 그 사이에 들어온 요청이 실패한다.

```aiignore
Pod Terminating
  ├─ Service 쪽 : 엔드포인트에서 제거 → 노드에 반영 → 새 요청이 더 오지 않음
  └─ 컨테이너 쪽: preStop(sleep 5) → SIGTERM → 처리 중 요청 마무리 후 종료
                  └ 이 사이 들어온 요청도 정상 처리
```

- **preStop**은 컨테이너에 SIGTERM을 내보내기 **직전에** 실행된다. `sleep 5`로 잠깐 버티면 그동안 엔드포인트 변경이 반영되어, 트래픽이 끊긴 뒤에 종료가 시작된다.
- **Graceful Shutdown** 은 SIGTERM을 받은 애플리케이션이 새 요청은 받지 않고 **처리 중인 요청은 끝까지 마친 뒤** 종료하는 것이다. Spring Boot는 `server.shutdown=graceful`로 켠다.
- preStop 시간과 Graceful Shutdown 시간은 모두 Pod의 `terminationGracePeriodSeconds` (기본 30초) 안에 들어가야 한다. 이 시간을 넘기면 SIGKILL로 강제 종료된다.

```properties
# Spring Boot 
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=20s
```

<br>

### 4.4 readinessProbe가 없으면

readinessProbe를 빼고 기동이 느린 앱(Spring Boot 등)으로 롤링 업데이트를 하면, 아직 요청이 처리할 수 없는 새 Pod가 트래픽을 받아 **502, connection refused** 같은 실패가 섞여 나온다. nginx는 1초 안에 떠서 차이가 잘 안보일 뿐이다.

<br>

### 4.5 실습 명령어

```bash
kubectl apply -f deployment.yaml
kubectl expose deployment my-app --port=80

# 터미널 1: 업데이트 중 응답 코드 계속 확인 (200만 나와야 한다.)
kubectl run curl --rm -it --image=curlimages/curl --restart=Never -- \
  sh -c 'while true; do curl -s -o /dev/null -w "%{http_code}\n" http://my-app; sleep 0.2; done'

# 터미널 2: 이미지 교체 → 롤링 업데이트
kubectl set image deployment/my-app my-container=nginx:1.27
kubectl annotate deployment/my-app kubernetes.io/change-cause="nginx 1.27 로 업데이트"
kubectl rollout status deployment/my-app
kubectl get pods -w                       # v2 Pod 가 Ready 된 뒤 v1이 하나씩 종료되는지

kubectl rollout history deployment/my-app # change-cause 기록 확인
kubectl rollout undo deployment/my-app    # 직전 버전으로 롤백
kubectl rollout pause deployment/my-app  # 업데이트 일시 정지 / resume 으로 재개
```

<br>
<hr>

### 핵심 정리
- **Service**는 바뀌는 Pod 앞에 고정 IP와 DNS 이름을 두고, 라벨 셀렉터로 대상을 골라 트래픽을 나눈다. 실제 전달은 kube-proxy가 한다.
- `port`는 service, `targetPort`는 컨테이너, `nodePort`는 노드의 포트다.
- **ClusterIP ⊂ NodePort ⊂ LoadBalancer.** 로드밸런서 구현체가 없으면 `EXTERNAL-IP`는 `<Pending>`이다.
- **Ingress는 규칙일 뿐이고, 실제 일은 Ingress Controller가 한다.** `ingressClassName`을 빠뜨리면 아무도 처리하지 않는다.
- `rewrite-target`은 리다이렉트가 아니라 백엔드로 넘길 때 경로를 바꾸는 것이다.
- **liveness 실패 → 재시작, readiness 실패 → 트래픽 제외, startup 성공 전 → 나머지 Probe 보류.** liveness에는 외부 의존성을 넣지 않는다.
- 무중단 배포 = **readinessProbe + 롤링 업데이트 옵션 + preStop/Graceful Shutdown.** 두 버전이 함께 뜨는 구간이 있으니 하위 호환을 지킨다.

<br>

