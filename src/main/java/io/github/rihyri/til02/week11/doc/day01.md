<br>

# Week 11 - Day 01. Kubernetes 아키텍처와 Minikube

<br>

이번 수업은 **컨테이너 오케스트레이션이 왜 필요한지 → Kubernetes 클러스트를 이루는 구성 요소(컨트롤 플레인, 워커 노드) → `kubectl apply` 한 번에 내부에서 일어나는 일 → 컨트롤 플레인을 홀수 대로 두는 이유 → Minikube와 kubectl로 로컬 클러스터 다루기** 순서로 진행됐다. 그 중 **구성 요소들이 API 서버를 중심으로 움직이는 방식**과 **etcd 과반수 (Quorum) 원리**가 특히 중요하다.

<br>
<hr>

## 1. 왜 Kubernetes인가

<br>

### 1.1 컨테이너 오케스트레이션

서버가 몇 대일 때는 `docker run`으로 충분하다. 하지만 서버가 수십, 수백 대가 되면 **어느 서버에 컨테이너를 몇 개 띄울지, 죽은 컨테이너를 누가 다시 띄울지, 새 버전으로 어떻게 바꿀지**를 사람이 하나 하나 정할 수 없다. 이 일을 대신 맡아 여러 서버의 컨테이너를 조율하는 것이 **컨테이너 오케스트레이션**이고, 그 대표 도구가 Kubernetes(줄여서 k8s, k와 s 사이 8글자)다.

<table>
    <thead>
        <tr>
			<th>Kubernetes가 해 주는 일</th>
			<th>내용</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>롤아웃과 롤백</td>
            <td>새 버전을 조금씩 교체하고, 문제가 생기면 이전 버전으로 되돌린다.</td>
        </tr>
        <tr>
            <td>서비스 디스커버리와 로드 밸런싱</td>
            <td>Pod IP가 바뀌어도 서비스 이름으로 찾고, 트래픽을 여러 Pod에 나눈다. (Eureka와 비슷한 역할)</td>
        </tr>
        <tr>
            <td>자동 빈 패킹</td>
            <td>컨테이너가 요청한 CPU, 메모리에 맞춰 여유 있는 노드에 배치한다.</td>
        </tr>
        <tr>
            <td>자동 복구</td>
            <td>죽은 컨테이너를 재시작하고, 노드가 죽으면 Pod를 다른 노드에 다시 띄운다.</td>
        </tr>
        <tr>
            <td>시크릿과 설정 관리</td>
            <td>비밀번호, API 키, 설정 값을 이미지와 분리해 관리한다.</td>
        </tr>
        <tr>
            <td>스토리지 오케스트레이션</td>
            <td>로컬 디스크, 클라우드 스토리지 등을 Pod에 연결해 준다.</td>
        </tr>
    </tbody>
</table>

<br>

## 1.2 명령형과 선언형

<table>
    <thead>
        <tr>
			<th>방식</th>
			<th>예</th>
			<th>누가 상태를 맞추나</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>명령형 (Docker)</td>
            <td>"nginx 컨테이너 하나 실행해"</td>
            <td>사람이 명령을 내린 그 순간에만</td>
        </tr>
        <tr>
            <td>선언형 (Kubernetes)</td>
            <td>"nginx Pod가 항상 3개 있어야 한다." (YAML)</td>
            <td>Kubernetes가 계속 현재 상태를 보며 차이를 메운다.</td>
        </tr>
    </tbody>
</table>

선언형은 에어컨의 희망 온도 설정과 같다. 온도를 정해 두면 에어컨이 알아서 켜졌다 꺼졌다 하며 그 온도를 유지한다. Pod가 하나 죽으면 "3개여야 하는데 2개"라는 차이가 생기고, Kubernetes가 새 Pod를 만들어 다시 3개로 만든다.

<br>
<hr>

## 2. 기본 단위: Node와 Pod

<br>

### 2.1 Node - 컨테이너가 실제로 도는 컴퓨터

**Node는 Kubernetes 클러스터에 속한 컴퓨터 한 대**다. 물리 서버일 수도 있고 가상 머신(VM)일 수도 있다. 각 노드에는 Pod를 실행하는 데 필요한 프로그램(kubelet, 컨테이너 런타임, kube-proxy)이 설치되어 있고, 컨트롤 플레인의 관리를 받는다.

- **컨트롤 플레인 노드 :** 클러스트를 관리하는 구성 요소가 도는 노드
- **워커 노드 :** 애플리케이션 Pod가 실제로 실행되는 노드

노드는 "물리 PC 한 대 = 노드 한 개"로 정해진 개념이 아니다. PC 한 대 위에 VM을 여러 개 띄우면 노드가 여러 개가 될 수도 있고, Minikube는 Docker 컨테이너(또는 VM) 하나를 노드 하나로 쓴다. 다만 **노드가 올라가 있는 머신이 꺼지면 그 노드도 함께 멈춘다.** 이때 노드 상태가 `NotReady`가 되고, Deployment로 관리되던 Pod는 다른 노드에 새로 만들어진다.

<br>
<hr>

### 2.2 Pod - Kubernetes가 다루는 가장 작은 단위

**Pod는 Kubernetes에서 만들고 관리할 수 있는 가장 작은 배포 단위**다. 하나 이상의 컨테이너를 묶은 것으로, 묶인 컨테이너들은 **같은 노드에서 함께 실행되고 네트워크(IP, 포트)와 스토리지를 공유한다.**

- Kubernetes는 컨테이너를 직접 다루지 않고 **Pod 단위로** 배치하고 관리한다.
- 대부분 Pod 하나에 컨테이너 하나를 넣는다. 로그 수집기처럼 메인 앺 옆에 꼭 붙어 있어야 하는 보조 컨테이너가 있을 때만 여러 개를 넣는다.
- 같은 Pod 안의 컨테이너는 IP를 공유하므로 서로 `localhost`로 통신한다.
- Pod 하나는 **반드시 노드 하나에서만** 실행된다. 여러 노드로 쪼개지지 않는다.
- Pod는 **일회용**이다. 죽은 Pod를 살리는 것이 아니라 **새 Pod(새 이름, 새 IP)를 만든다.** 그래서 Pod를 직접 만들기보다 Deployment에 맡기고, IP대신 service 이름으로 접근한다.

<br>

### 2.3 한 눈에 보기

```aiignore
Cluster
 ├─ Control Plane Node  : apiserver, etcd, scheduler, controller-manager
 ├─ Worker Node 1
 │    ├─ Pod (IP 10.244.0.5)
 │    │    └─ Container: nginx
 │    └─ Pod (IP 10.244.0.6)
 │         ├─ Container: app
 │         └─ Container: log-agent   ← 같은 IP, localhost 로 통신
 └─ Worker Node 2
      └─ Pod ...
```

<table>
    <thead>
        <tr>
			<th>Docker에서</th>
			<th>Kubernetes에서</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>Docker가 설치된 서버(호스트)</td>
            <td>Node</td>
        </tr>
        <tr>
            <td>컨테이너</td>
            <td>Pod 안의 컨테이너 (관리 단위는 Pod)</td>
        </tr>
        <tr>
            <td>docker run</td>
            <td>Pod 명세(YAML)을 API 서버로 제출</td>
        </tr>
    </tbody>
</table>

<br>

```yaml
# 가장 단순한 Pod 명세
apiVersion: v1
kind: Pod
metadata:
  name: nginx
spec:
  containers:
    - name: nginx
      image: nginx:1.27
      ports:
        - containerPort: 80
```

```bash
kubectl get nodes -o wide       # 노드 목록과 IP, OS, 컨테이너 런타임
kubectl get pods -o wide        # 각 Pod의 IP와 어느 노드(NODE)에서 도는지
```

<br>

## 3. Kubernetes 아키텍처

<br>

### 3.1 전체 구조

```aiignore
                 kubectl / 사용자
                        │ REST API (HTTPS)
┌─ Control Plane ───────▼──────────────────────────────┐
│  etcd ◀──▶ kube-apiserver ◀── kube-scheduler          │
│            (모든 요청의 관문) ◀── kube-controller-manager │
└───────────────────────▲──────────────────────────────┘
                        │ watch / 상태 보고
┌─ Worker Node 1 ───────┴───┐   ┌─ Worker Node 2 ──────────┐
│ kubelet → 컨테이너 런타임  │   │ kubelet → 컨테이너 런타임 │
│            → Pod           │   │            → Pod          │
│ kube-proxy (Service 규칙)  │   │ kube-proxy                │
└───────────────────────────┘   └──────────────────────────┘
```

회사에 비유하면 컨트롤 플레인은 업무를 나누고 진행 상황을 챙기는 관리 부서, 워커 노드는 실제 일을 하는 현장 팀이다. 서비스가 켜지면 주로 워커 노드를 늘린다. 컨트롤 플레인 노드에는 기본적으로 **taint**(일반 Pod를 배치하지 말라는 표시)가 걸려 있어 애플리케이션 Pod가 올라가지 않는다.

**구성 요소끼리 서로 직접 명령하지 않는다.** 모든 정보는 API 서버에 모이고, 각 구성 요소는 API 서버를 **지켜보다가 (watch)** 자기 일을 발견하면 처리하고 결과를 다시 API 서버에 기록한다. 게시판에 업무가 올라오면 담당자가 가져가서 처리하고 결과를 다시 게시판에 적는 방식과 같다.

<br>

### 3.2 컨트롤 플레인 구성 요소

<table>
    <thead>
        <tr>
			<th>구성 요소</th>
			<th>역할</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>kube-apiserver</td>
            <td>클러스터의 유일한 관문. kubectl 요청과 구성 요소 간 요청이 모두 여기를 거친다. etcd와 직접 통신하는 유일한 구성 요소</td>
        </tr>
        <tr>
            <td>etcd</td>
            <td>클러스터의 모든 상태를 저장하는 분산 키-값 저장소. Raft 합의 알고리즘으로 여러 etcd가 같은 데이터를 유지한다. 운영 환경에서는 정기 백업 필수</td>
        </tr>
        <tr>
            <td>kube-scheduler</td>
            <td>아직 노드가 정해지지 않은 Pod를 어느 노드에 둘지 정한다.</td>
        </tr>
        <tr>
            <td>kube-controller-manager</td>
            <td>여러 컨트롤러를 한 프로세스로 실행하며, 현재 상태를 원하는 상태로 맞춘다.</td>
        </tr>
    </tbody>
</table>

<br>

#### · API 서버의 요청 처리 순서

```aiignore
요청 → ① 인증(누구인가) → ② 인가(권한이 있나)
     → ③ 어드미션 컨트롤(정책 확인, 기본값 채우기) → ④ 검증(형식, 필드 값) → etcd 저장/조회
```

#### · 스케줄러의 노드 선택

1. **필터링 :** 자원이 부족하거나 조건(노드 셀렉터 등)이 맞지 않는 노드를 제외한다.
2. **스코어링 :** 남은 노드에게 점수를 매겨 가장 높은 노드를 고른다.

스케줄러는 고른 노드의 이름을 API 서버에 **기록만** 한다. 컨테이너를 실제로 띄우는 것은 그 노드의 kubelet이다. 조건에 맞는 노드가 하나도 없으면 Pod는 **`Pending`** 상태로 남는다.

#### · 컨트롤러 매니저 안의 대표 컨트롤러

<table>
    <thead>
        <tr>
			<th>컨트롤러</th>
			<th>하는 일</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>노드 컨트롤러</td>
            <td>노드 장애 감지</td>
        </tr>
        <tr>
            <td>디플로이먼트 컨트롤러</td>
            <td>Deployment를 보고 ReplicaSet 생성, 롤링 업데이트</td>
        </tr>
        <tr>
            <td>레플리카셋 컨트롤러</td>
            <td>지정한 개수만큼 Pod 유지</td>
        </tr>
        <tr>
            <td>엔드포인트 슬라이스 컨트롤러</td>
            <td>Service와 Pod 연결</td>
        </tr>
    </tbody>
</table>

<br>

### 3.3 노드 구성 역할

<table>
    <thead>
        <tr>
			<th>구성 요소</th>
			<th>역할</th>
			<th>실행 형태</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>kubelet</td>
            <td>자기 노드에 배정된 Pod를 발견해 컨테이너를 실행하고, 노드와 Pod 상태를 API 서버에 계속 보고한다.</td>
            <td>노드의 일반 프로세스(systemd 서비스). Pod 목록에는 안 보인다.</td>
        </tr>
        <tr>
            <td>kube-proxy</td>
            <td>Service의 가상 IP나 NodePort로 온 요청을 실제 Port로 전달하는 규칙을 리눅스 커널(iptable, IPVS 등)에 설정한다.</td>
            <td>대부분 DaemonSet Pod로 실행. kube-system에 보인다.</td>
        </tr>
        <tr>
            <td>컨테이너 런타임</td>
            <td>이미지를 받고 컨테이너를 생성, 실행, 중지한다. CRI 규격을 따르는 containerd, CRI-O 등</td>
            <td>노드의 프로세스</td>
        </tr>
    </tbody>
</table>

- kube-proxy는 규칙을 커널에 **미리 깔아 두는** 역할이라, kube-proxy가 잠깐 멈춰도 이미 깔린 규칙으로 통신은 계속된다. (Pod 목록이 바뀐 것은 반영되지 않는다.)
- Docker로 빌드한 이미지는 OCI 표준 이미지라서 containerd 같은 런타임에도 그대로 실행된다.
- 
```bash
kubectl get pods -n kube-system     # apiserver, etcd, scheduler, kube-proxy 등은 보이지만 kubelet 은 없다
```

<br>
<hr>

## 4. `kubectl apply` 하면 일어나는 일 ⭐

<br>

`replicas: 2`인 Deployment를 적용했을 때의 흐름이다.

```aiignore
kubectl      API Server      etcd     Controller    Scheduler    kubelet
   │──① 생성 요청──▶│              │           │             │           │
   │               │──② 저장────▶│           │             │           │
   │               │◀──③ ReplicaSet, Pod 2개 생성 (노드 미정)│           │
   │               │◀────────────④ Pod 를 둘 노드 기록──────│           │
   │               │◀──────────────⑤ 내 노드에 배정된 Pod 발견──────────│
   │               │                                    ⑥ 런타임으로 컨테이너 실행
   │               │◀──────────────⑦ Pod 상태 Running 보고──────────────│
```

1. kubectl이 Deployment 생성 요청을 API 서버에 보낸다.
2. API 서버가 요청을 검사하고 etcd에 저장한다. **이 시점에는 컨테이너가 하나도 없다.**
3. 디플로이먼트 컨트롤러가 ReplicaSet을 만들고, 레플리카셋 컨트롤러가 Pod 2개를 만든다. 아직 노드는 정해지지 않았다.
4. 스케줄러가 노드가 비어 있는 Pod를 발견하고, 각 Pod의 노드를 정해 API 서버에 기록한다.
5. 해당 노드의 kubelet이 자기에게 배정된 Pod를 발견한다.
6. kubelet이 컨테이너 런타임을 통해 이미지를 받아 컨테이너를 실행한다.
7. kubelet이 Pod 상태를 `Running`으로 보고한다.

모든 단계가 **API 서버를 거쳐** 이어진다는 점이 핵심이다. 직접 확인해 보면 다음과 같다.

```bash
kubectl create deployment web --image=nginx:1.27 --replicas=2
kubectl get deploy,rs,pods -o wide                         # Deployment → ReplicaSet → Pod 연결 확인
kubectl get events --sort-by=.metadata.creationTimestamp   # Scheduled → Pulling → Created → Started 순서 확인
```

<br>
<hr>

## 5. 컨트롤 플레인은 왜 3대 이상, 홀수인가 ⭐⭐

<br>

### 5.1 여러 대일 때 누가 결정하나

<table>
    <thead>
        <tr>
			<th>구성 요소</th>
			<th>방식</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>kube-apiserver</td>
            <td>액티브-액티브. 모든 마스터가 요청을 처리하고, 앞의 로드 밸런서를 둔다.</td>
        </tr>
        <tr>
            <td>kube-scheduler, kube-controller-manager</td>
            <td>리더 선출. 한 대만 리더로 실제 결정을 내리고 나머지는 대기한다. 리더가 죽으면 다른 대가 이어받는다.</td>
        </tr>
    </tbody>
</table>

스케줄러가 동시에 여러 대 움직이면 같은 Pod를 서로 다른 노드에 배정하는 충돌이 생길 수 있어서 리더 한 대만 일을 한다.

<br>

### 5.2 홀수인 이유 - etcd의 과반수

etcd는 **전체의 과반수가 동의해야 쓰기를 확정**한다.

- **Split-Brain 방지 :** 2대가 네트워크를 끊기면 둘 다 자기가 리더라고 생각해 데이터가 갈라질 수 있다. 과반수 규칙이 있으면 과반을 확보한 쪽만 쓰기를 할 수 있다.
- **과반을 잃으면 쓰기를 멈춘다 :** 데이터가 오염되는 것보다 멈추는 쪽을 택한다. 이미 실행 중인 Pod는 계속 돌지만, 새 배포나 변경은 할 수 없다.


| etcd 수 | 과반수 | 버틸 수 있는 장애 |
|---|---|---|
| 1 | 1 | 0 |
| 2 | 2 | 0 |
| 3 | 2 | **1** |
| 4 | 3 | 1 |
| 5 | 3 | **2** |

**짝수로 한 대 늘려도 버틸 수 있는 장애 수는 그대로**이고 서버 비용만 늘어난다. 그래서 3대나 5대로 구성한다.

<br>

### 5.3 API 서버 앞의 로드 밸런서 (온프레미스)

물리 서버에 직접 구축할 때는 **HAProxy + Keepalived** 조합을 많이 쓴다.

- **HAProxy :** 여러 API 서버로 요청을 나누는 로드 밸런서
- **Keepalived :** 마스터들이 **가상 IP(VIP)** 하나를 공유하게 하고, VIP를 가진 마스터가 죽으면 다른 마스터로 VIP를 넘긴다. (Failover)

워커 노드와 kubectl은 VIP만 바라보므로, 마스터 한 대가 죽어도 접속 주소가 바뀌지 않고 통신이 이루어진다.

<br>
<hr>

## 6. Minikube와 kubectl

<br>

### 6.1 두 도구의 역할

<table>
    <thead>
        <tr>
			<th>도구</th>
			<th>역할</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>Minikube</td>
            <td>로컬 PC에 연습용 클러스터를 만들고, 멈추고, 지우는 도구. 기본은 노드 하나다 컨트롤 플레인과 워커를 함께 맡는다.</td>
        </tr>
        <tr>
            <td>kubectl</td>
            <td>만들어진 클러스터에 명령을 내리는 CLI. `~/.kube/config`에 적힌 API 서버로 HTTPS 요청을 보낸다.</td>
        </tr>
    </tbody>
</table>

Minikube 클러스터도 구성 요소는 운영 클러스터와 같으므로, 여기서 익힌 명령과 YAML은 운영 환경에서도 그대로 쓴다. (`--nodes` 옵션으로 여러 노드도 만들 수 있다.)

```bash
minikube start --driver=docker        # 클러스터 생성/시작 (kubeconfig 자동 설정)
minikube status                       # 상태 확인
minikube stop                         # 멈춤 (데이터 유지)
minikube delete                       # 클러스터 삭제
```

<br>

### 6.2 kubectl 명령 구조

```aiignore
kubectl [command] [TYPE] [NAME] [flags]
kubectl   get      pods   my-pod  -n dev -o wide
```

<table>
    <thead>
        <tr>
			<th>자리</th>
			<th>의미</th>
			<th>예</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>command</td>
            <td>무엇을 할지</td>
            <td>get, describe, apply, delete, logs, exec</td>
        </tr>
        <tr>
            <td>TYPE</td>
            <td>어떤 자원인지 (단수, 복수, 약어 모두 가능)</td>
            <td>pod / pods / po, deploy, svc</td>
        </tr>
        <tr>
            <td>NAME</td>
            <td>자원 이름. 생략하면 해당 타입 전체</td>
            <td>my-pod</td>
        </tr>
        <tr>
            <td>flags</td>
            <td>부가 옵션</td>
            <td>아래 표</td>
        </tr>
    </tbody>
</table>

<table>
    <thead>
        <tr>
			<th>자주 쓰는 flag</th>
			<th>의미</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>-n <ns> / -A</td>
            <td>네임스페이스 지정 (생략 시 default) / 모든 네임스페이스</td>
        </tr>
        <tr>
            <td>-o wide / -o yaml</td>
            <td>IP, 노드까지 표시 / 전체 YAML 출력</td>
        </tr>
        <tr>
            <td>-l app=nginx</td>
            <td>라벨로 필터링</td>
        </tr>
        <tr>
            <td>-w</td>
            <td>변화가 있을 때마다 계속 출력</td>
        </tr>
        <tr>
            <td>--dry-run=client -o yaml</td>
            <td>실제로 만들지 않고 YAML만 출력 (매니페스트 초안 만들기)</td>
        </tr>
    </tbody>
</table>

<br>

### 6.3 용도별 주요 명령

<table>
    <thead>
        <tr>
			<th>용도</th>
			<th>명령</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>생성, 수정, 삭제</td>
            <td>kubectl apply -f app.yaml, kubectl delete -f app.yaml</td>
        </tr>
        <tr>
            <td>조회, 상태</td>
            <td>kubectl get pods, kubectl describe pod <이름></td>
        </tr>
        <tr>
            <td>사용량</td>
            <td>kubectl top nodes, kubectl top pods (metrics-server 필요)</td>
        </tr>
        <tr>
            <td>디버깅</td>
            <td>kubectl logs <pod>, kubectl exec -it <pod> -- /bin/sh, kubectl get events</td>
        </tr>
        <tr>
            <td>로컬 접근</td>
            <td>kubectl port-forward pod/<pod> 8080:80</td>
        </tr>
        <tr>
            <td>롤백</td>
            <td>kubectl rollout undo deployment/<이름></td>
        </tr>
    </tbody>
</table>

```bash
# YAML 초안 만들기 → 수정 → 적용
kubectl create deployment web --image=nginx:1.27 --dry-run=client -o yaml > web.yaml
kubectl apply -f web.yaml
```

<br>

### 6.4 여러 클러스터 바꿔 쓰기 (context)

`~/.kube/config` 에는 클러스터가 여러 개 등록될 수 있고, 그중 지금 명령을 보낼 대상을 **context**로 고른다. 회사 클러스터와 Minikube를 함께 쓸 때 **현재 context를 확인하는 습관**이 중요하다. 연습용 명령을 운영 클러스터에 보내는 실수를 막을 수 있다.

```bash
kubectl config get-contexts         # 등록된 context 목록 (* 가 현재)
kubectl config current-context      # 현재 대상
kubectl config use-context minikube # 대상 변경
```

<br>
<hr>

## 7. 클러스터 상태 확인

<table>
    <thead>
        <tr>
			<th>명령</th>
			<th>확인할 것</th>
			<th>정상 기준</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>minikube status</td>
            <td>host, kubelet, apiserver, kubeconfig</td>
            <td>모두 Running 또는 Configured</td>
        </tr>
        <tr>
            <td>kubectl get nodes</td>
            <td>노드 상태</td>
            <td>STATUS가 Ready 여야 Pod가 배치된다.</td>
        </tr>
        <tr>
            <td>kubectl top nodes / top pods</td>
            <td>CPU, 메모리 사용량</td>
            <td>숫자가 나온다. (metrics-server 필요)</td>
        </tr>
        <tr>
            <td>minikube addons list</td>
            <td>애드온 리스트</td>
            <td>필요한 애드온이 enabled</td>
        </tr>
        <tr>
            <td>minikube dashboard</td>
            <td>웹 화면으로 클러스터 확인</td>
            <td>브라우저가 열림 (종료는 Ctrl + C)</td>
        </tr>
    </tbody>
</table>

```bash
minikube addons enable metrics-server   # kubectl top 을 쓰려면 먼저 켠다. (수집까지 1분쯤 걸린다.)
kubectl top nodes

kubectl get pods -A                     # 모든 네임스페이스의 Pod
kubectl describe pod <이름>             # 맨 아래 Events 에서 Pending, 이미지 pull 실패 원인 확인
```

<br>
<hr>

## 8. 핵심 정리

- **Node**는 클러스터에 속한 컴퓨터(물리 또는 VM)이고, **Pod**는 하나 이상의 컨테이너를 묶은 Kubernetes의 최소 배포 단위다. Pod는 한 노드에서만 돌고, 죽으면 새로 만들어진다.
- Kubernetes는 **선언형**이다. 원하는 상태를 YAML로 적어 두면 컨트롤러가 계속 현재 상태를 맞춘다.
- 컨트롤 플레인은 apiserver, etcd, scheduler, controller-manager로 이뤄지고, 노드에는 kubelet, kube-proxy, 컨테이너 런타임이 있다.
- **모든 구성 요소는 API 서버를 통해서만** 소통하고, etcd와 직접 이야기하는 것은 API 서버 뿐이다.
- 스케줄러는 노드를 **정하기만** 하고, 실제 실행은 kubelet이 한다. 맞는 노드가 없으면 `Peding`.
- etcd는 **과반수**가 있어야 쓰기를 한다. 그래서 컨트롤 플레인은 **3, 5대처럼 홀수**로 둔다.
- Minikube는 클러스터를 만드는 도구, kubectl은 클러스터에 명령하는 도구다. 명령 전에 **현재 context**를 확인한다.

<br>

