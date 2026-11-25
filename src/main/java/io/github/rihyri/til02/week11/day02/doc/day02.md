<br>

# Week 11 - Day 02. Pod와 Deployment, Namespace

<br>

이번 수업은 **Pod를 관리 단위로 다시 보기 → Pod 생명주기와 상태 읽기 → Pod를 대신 관리해 주는 Deployment → 라벨과 셀렉터로 Pod를 찾는 방법 → Deployment YAML과 조회 결과 읽기 → 클러스터 밖에서 접근하기 → Namespace로 자원 나누기** 순서로 진행됐다. 그 중 **Pod 상태(STATUS)를 보고 원인을 찾는 방법**과 **Deployment → ReplicaSet → Pod 관계**가 특히 중요하다.

<br>
<hr>

## 1. Pod 다시 보기

<br>

### 1.1 Kubernetes의 관리 단위는 Pod

Docker에서 컨테이너 하나가 관리 단위였지만, Kubernetes는 컨테이너를 항상 **Pod로 감싸서** 다룬다. 스케줄러가 노드를 정해주는 대상, kubelet이 실행하는 대상, Deployment가 개수를 세는 대상 모두 Pod다.

Pod안의 컨테이너들은 **항상 같은 노드에 함께 배치**된다. 한 Pod의 컨테이너가 여러 노드로 나뉘는 일은 없다.

<br>

### 1.2 Pod의 세 가지 특징

<table>
    <thead>
        <tr>
            <th>특징</th>
            <th>내용</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>리소스 공유</td>
            <td>같은 Pod의 컨테이너는 IP, 포트 공간, 볼륨을 함께 쓴다. 서로 localhost로 통신한다.</td>
        </tr>
        <tr>
            <td>Pod 하나 = 앱 인스턴스 하나</td>
            <td>보통 메인 컨테이너 하나만 넣는다. 로그 수집기나 프록시처럼 메인을 돕는 사이드카만 같이 넣는다.</td>
        </tr>
        <tr>
            <td>임시성</td>
            <td>Pod는 일회용이다. 그래서 Pod IP 대신 Service로 접근하고, 남겨야 할 데이터는 볼륨에 둔다.</td>
        </tr>
    </tbody>
</table>

<br>

### 1.3 "컨테이너 재시작"과 "Pod 교체"는 다르다.

<table>
    <thead>
        <tr>
			<th>구분</th>
			<th>무엇이 바뀌나</th>
			<th>언제 일어나나</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>컨테이너 재시작</td>
            <td>Pod는 그대로. 이름과 IP 유지, RESTARTS 숫자만 올라간다.</td>
            <td>컨테이너 프로세스가 죽었을 때 kubelet이 같은 Pod 안에서 다시 띄움</td>
        </tr>
        <tr>
            <td>Pod 교체</td>
            <td>새 Pod가 생긴다. 이름과 IP가 바뀐다.</td>
            <td>Pod 삭제, 노드 장애, 새 버전 배포</td>
        </tr>
    </tbody>
</table>

```bash
kubectl get pods -o wide          # RESTARTS(재시작 횟수), IP, NODE 확인
```

<br>
<hr>

## 2. Pod 생명주기와 상태 읽기 ⭐

<br>

### 2.1 Phase - 생명주기의 큰 단계

Phase는 Pod가 지금 생명주기의 어디쯤 있는지를 나타내는 값으로, 다섯 가지뿐이다. 정상적인 웹 서버는 `pending`을 지나 `Running`에 계속 머문다.

<table>
    <thead>
        <tr>
			<th>Phase</th>
			<th>의미</th>
			<th>메모</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>Pending</td>
            <td>Pod가 만들어졌지만 컨테이너가 아직 실행 전</td>
            <td>노드를 고르는 중이거나 이미지를 받는 중. 오래 머물면 describe의 Events에서 Insufficient cpu, Insufficient memory 같은 원인을 찾는다.</td>
        </tr>
        <tr>
            <td>Running</td>
            <td>노드에 배정됐고 컨테이너가 생성됐으며, 하나 이상이 실행 중이거나 재시작 중</td>
            <td>Running이어도 요청을 받을 준비가 끝났다는 뜻은 아니다. 준비 여부는 READY 열(1/1)과 readinessProbe로 판단한다.</td>
        </tr>
        <tr>
            <td>Succeeded</td>
            <td>모든 컨테이너가 종료 코드 0으로 끝났고 다시 시작하지 않음</td>
            <td>Job 같은 일회성 작업에서 본다.</td>
        </tr>
        <tr>
            <td>Failed</td>
            <td>모든 컨테이너가 끝났고, 하나 이상이 0이 아닌 코드로 실패</td>
            <td>Job처럼 재시작하지 않는 Pod에서 본다.</td>
        </tr>
        <tr>
            <td>Unknown</td>
            <td>상태를 알 수 없음</td>
            <td>주로 노드와 API 서버 사이의 통신이 끊겼을 때</td>
        </tr>
    </tbody>
</table>

Deployment가 만든 Pod는 `restartPolicy: Always`라서 컨테이너가 실패해도 계속 재시작한다. 그래서 `Failed`가 되지 않고 `CrashLoopBackOff`로 보인다.

<br>

### 2.2 종료 코드 읽기

128보다 큰 종료 코드는 **128 + 시그널 번호**로 읽는다.

<table>
    <thead>
        <tr>
			<th>종료 코드</th>
			<th>의미</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>0</td>
            <td>정상 종료</td>
        </tr>
        <tr>
            <td>1</td>
            <td>애플리케이션 오류</td>
        </tr>
        <tr>
            <td>137</td>
            <td>128 + 9(SIGKILL), 강제 종료. 대부분 메모리 제한을 넘은 OOMKilled</td>
        </tr>
        <tr>
            <td>143</td>
            <td>128 + 15(SIGTERM), 삭제나 재배포로 종료 신호를 받고 정상적으로 내려감</td>
        </tr>
    </tbody>
</table>

```bash
kubectl describe pod <pod>      
# Last State: Terminated / Reason: OOMKilled / Exit Code: 137 처럼 나온다.
```

<br>

### 2.3 STATUS 열에서 자주 보이는 값

`kubectl get pods`의 `STATUS` 열에는 Phase보다 **컨테이너가 왜 그 상태인지**가 더 자주 찍힌다.

<table>
    <thead>
        <tr>
			<th>STATUS</th>
			<th>의미</th>
			<th>확인할 것</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>ContainerCreating</td>
            <td>이미지를 받거나 볼륨을 붙이는 중</td>
            <td>잠시 기다린다. 길어지면 describe</td>
        </tr>
        <tr>
            <td>ImagePullBackOff / ErrImagePull</td>
            <td>이미지를 받지 못함</td>
            <td>이미지 이름, 태그 오타 / 비공개 레지스트리 인증 / CPU 아키텍처</td>
        </tr>
        <tr>
            <td>CrashLoopBackOff</td>
            <td>시작하자마자 죽기를 반복. 재시작 대기 시간이 10초, 20초, 40초...로 늘어나 최대 5분까지 길어짐</td>
            <td>kubectl logs <pod> --previous 로 직전에 죽은 컨테이너의 로그 확인</td>
        </tr>
        <tr>
            <td>OOMKilled</td>
            <td>메모리 제한을 넘어 강제 종료</td>
            <td>resources.limits.memory 와 실제 사용량 비교</td>
        </tr>
        <tr>
            <td>Completed</td>
            <td>컨테이너가 exit 0으로 끝남</td>
            <td>웹 서버인데 이 상태라면 왜 프로세스가 끝났는지 확인</td>
        </tr>
    </tbody>
</table>

<br>

### 2.4 문제가 생긴 Pod를 볼 때의 순서

```bash
kubectl get pods                    # 1. STATUS, RESTARTS 확인
kubectl describe pod <pod>          # 2. 맨 아래 Events 에서 원인 확인 (스케줄링, 이미지 pull 실패 등)
kubectl logs <pod>                  # 3. 애플리케이션 로그
kubectl logs <pod> --previous       # 4. 재시작 중이면 이전 컨테이너 코드
kubectl get pods -w                 # 상태 변화를 계속 지켜보기
```

`describe`는 **Kubernetes가 본 원인**(스케줄링, 이미지, 자원)을 `logs`는 **애플리케이션이 남긴 원인**을 보여준다. 둘을 같이 봐야 한다.

<br>
<hr>

## 3. Deployment

<br>

### 3.1 Deployment가 필요한 이유

Pod는 일회용이라 죽으면 스스로 돌아오지 않는다. 그래서 Pod를 직접 만들지 않고, **Pod를 지켜보다가 부족하면 새로 만들어 주는 관리자**에게 원하는 상태를 맡긴다. 그 관리자가 Deployment다.

"nginx 이미지로 Pod 3개를 유지한다"처럼 원하는 상태를 YAML로 선언하면, Kubernetes가 현재 상태를 거기에 맞춘다. Pod가 사라지면 새로 만들고, 이미지를 바꾸면 순서대로 교체한다.

Deployment는 **상태가 없는(stateless)** 애플리케이션에 맞다. DB처럼 상태가 있는 애플리케이션은 StatefulSet을 쓴다.

<br>

### 3.2 Deployment, ReplicaSet, Pod의 관계 ⭐

```aiignore
Deployment: nginx-deployment          ← 버전 교체(롤링 업데이트), 롤백 담당
  │  replicas: 3, image: nginx:1.27
  │ 생성, 관리
  ▼
ReplicaSet: nginx-deployment-7c5ddbdf54   ← app=nginx 라벨의 Pod 를 3개로 유지
  │
  ├─ Pod: nginx-deployment-7c5ddbdf54-abcde
  ├─ Pod: nginx-deployment-7c5ddbdf54-fghij
  └─ Pod: nginx-deployment-7c5ddbdf54-klmno
        └ 이름 = Deployment 이름 - ReplicaSet 해시 - 무작위 문자열
```

<table>
    <thead>
        <tr>
			<th>리소스</th>
			<th>하는 일</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>Deployment</td>
            <td>ReplicaSet을 관리한다. 이미지가 바뀌면 새 ReplicaSet을 만들고 Pod를 조금씩 옮긴다.</td>
        </tr>
        <tr>
            <td>ReplicaSet</td>
            <td>라벨이 맞는 Pod를 정해진 개수만큼 유지하는 일만 한다.</td>
        </tr>
        <tr>
            <td>Pod</td>
            <td>실제 컨테이너가 도는 단위</td>
        </tr>
    </tbody>
</table>


팀에 비유하면 Deployment는 인력 계획을 세우는 팀장, ReplicaSet은 인원 수를 맞추는 담당자, Pod는 실제로 일하는 팀원이다. 보통 ReplicaSet이나 Pod는 직접 만들지 않고 **Deployment**만 만든다.

<br>

### 3.3 Deployment가 주는 기능

<table>
    <thead>
        <tr>
			<th>기능</th>
			<th>내용</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>롤아웃과 롤백</td>
            <td>이미지를 바꾸면 롤링 업데이트를 자동으로 하고, 문제가 생기면 이전 버전으로 되돌린다.</td>
        </tr>
        <tr>
            <td>스케일링</td>
            <td>replicas 값만 바꾸면 Pod 수가 늘거나 준다.</td>
        </tr>
        <tr>
            <td>자동 복구</td>
            <td>Pod가 지워지거나 노드 장애로 사라지면 ReplicaSet이 바로 새 Pod를 만든다.</td>
        </tr>
        <tr>
            <td>버전 기록</td>
            <td>이전 ReplicaSet을 revisionHistoryLimit (기본 10)개까지 남겨두어 언제든 돌아갈 수 있다.</td>
        </tr>
    </tbody>
</table>

```bash
# 스케일링
kubectl scale deployment nginx-app --replicas=3
# 이미지 교체 → 롤링 업데이트
kubectl set image deployment/nginx-app nginx-app=nginx:1.27
# 업데이트 진행 상황
kubectl rollout status deployment/nginx-app
# 버전 기록
kubectl rollout history deployment/nginx-app
# 직전 버전으로 롤백
kubectl rollout undo deployment/nginx-app
# 이미지 교체 후 ReplicaSet 이 2개가 된 것 확인
kubectl get rs
```

<br>
<hr>

## 4. 라벨과 셀렉터 - Deployment가 자기 Pod를 찾는 방법

Pod 이름은 새로 만들어질 때마다 바뀌기 때문에, Deployment와 ReplicaSet은 Pod를 이름으로 기억하지 않는다. 대신 Pod에 붙은 **라벨(키-값 꼬리표)** 을 보고 "내가 관리할 Pod"를 고른다.

```yaml
spec:
  selector:
    matchLabels:
      app: nginx            # ② 이 라벨을 가진 Pod 를 내 Pod 로 관리한다.
  template:
    metadata:
      labels:
        app: nginx          # ① 이 템플릿으로 만든 Pod 에 app=nginx 라벨이 붙는다.
```

- `selector.matchLabels`와 `template.metadata.labels`는 **반드시 일치**해야 한다. 다르면 `selector does not match template labels` 오류로 생성 자체가 거부된다. 자기가 만든 Pod를 자기가 찾지 못하는 상황을 미리 만드는 것이다.
- Service도 같은 방식으로 라벨을 보고 트래픽을 보낼 Pod를 찾는다. 라벨은 리소스끼리 연결하는 **접착제** 역할을 한다.

```bash
kubectl get pods --show-labels          # Pod 에 붙은 라벨 보기
kubectl get pods -l app=nginx           # 라벨로 골라 보기 (셀렉터)
```

<br>
<hr>

## 5. Deployment 만들고 읽기

<br>

### 5.1 실행 방법 두 가지

<table>
    <thead>
        <tr>
			<th>방식</th>
			<th>명령</th>
			<th>만들어지는 것</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>명령형</td>
            <td>kubectl run nginx --image=nginx</td>
            <td>Pod 하나. 관리하는 컨트롤러가 없어서 지우면 다시 생기지 않는다.</td>
        </tr>
        <tr>
            <td>명령형</td>
            <td>kubectl create deployment nginx-cli --image=nginx</td>
            <td>Deployment (→ ReplicaSet → Pod)</td>
        </tr>
        <tr>
            <td>선언형</td>
            <td>kubectl apply -f nginx-app.yaml</td>
            <td>YAML에 적힌 리소스. 파일로 남아 기록, 리뷰, 재사용이 쉽다.</td>
        </tr>
    </tbody>
</table>

<br>

### 5.2 Deployment 템플릿 읽기

YAML이 기본 뼈대(`apiVersion`, `kind`, `metadata`, `spec`) 안에 **Pod 설계도인 `template`** 이 한 겹 더 들어간다.

```yaml
# nginx-app.yaml
apiVersion: apps/v1             # Deployment 는 apps 그룹의 v1
kind: Deployment
metadata:
  name: nginx-app               # Deployment 이름
  labels:
    app: nginx-app              # Deployment 자신에게 붙는 라벨
spec:
  replicas: 1                   # 유지할 Pod 수
  selector:
    matchLabels:
      app: nginx-app            # 관리할 Pod 를 고르는 조건
  template:
    metadata:
      labels:
        app: nginx-app          # selector 와 같아야 한다.
    spec:
      containers:
        - name: nginx-app
          image: nginx
          ports:
            - containerPort: 80    # 정보 제공용
```

- `template` 아래는 Pod YAML 과 거의 같다. Deployment는 이 설계도로 Pod를 찍어낸다.
- `containerPort`는 **기록하지 않아도 컨테이너는 그 포트를 연다.** 기록하면 읽는 사람이 포트를 알 수 있고, `kubectl expose`가 포트를 자동으로 가져올 수 있다.

```bash
kubectl apply -f nginx-app.yaml
kubectl get deploy, rs, pods              # Deployment → ReplicaSet → Pod 한 번에 확인
```

<br>

### 5.3 `kubectl get deployment` 결과 읽기

```aiignore
NAME        READY   UP-TO-DATE   AVAILABLE   AGE
nginx-cli   1/1     1            1           31m
nginx-app   1/1     1            1           20s
```

<table>
    <thead>
        <tr>
			<th>열</th>
			<th>묻는 것</th>
			<th>의미</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>READY ⭐</td>
            <td>준비됐나?</td>
            <td>준비된 Pod 수 / 원하는 Pod 수. 0/1이 계속되면 Pod의 STATUS를 본다.</td>
        </tr>
        <tr>
            <td>UP-TO-DATE</td>
            <td>최신 버전인가?</td>
            <td>현재 템플릿(이미지, 설정)으로 만들어진 Pod 수. 롤링 업데이트 중에 점점 늘어난다.</td>
        </tr>
        <tr>
            <td>AVAILABLE</td>
            <td>요청을 받을 수 있나?</td>
            <td>준비 상태가 minReadySeconds 만큼 유지된 Pod 수</td>
        </tr>
        <tr>
            <td>AGE</td>
            <td></td>
            <td>생성 후 지난 시간</td>
        </tr>
    </tbody>
</table>

평소에는 세 숫자가 같고, **업데이트 중이거나 문제가 있을 때 값이 갈라진다.**

<br>

### 5.4 삭제 범위

```bash
kubectl delete deployment nginx-app       # ReplicaSet, Pod 도 함께 사라진다.
kubectl delete service nginx-app          # Service 는 별개 리소스라 따로 지운다.
```

<br>
<hr>

## 6. 클러스터 밖에서 접근하기

컨테이너가 80 포트로 떠 있어도 내 PC에서 `localhost:80` 으로 들어갈 수 없다. Pod 네트워크 (예: `10.244.x.x`)는 **클러스터 안에서만** 쓰는 주소이기 때문이다. Docker에서 `-p` 없이 띄운 컨테이너에 밖에서 접속할 수 없었던 것과 같다.

<table>
    <thead>
        <tr>
			<th>방법</th>
			<th>용도</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>Service (NodePort, LoadBalancer)</td>
            <td>클러스터 밖으로 Pod를 노출하는 정식 방법</td>
        </tr>
        <tr>
            <td>Ingress</td>
            <td>HTTP 경로, 도메인 기준으로 여러 Service에 나눠 보내기</td>
        </tr>
        <tr>
            <td>kubectl port-forward</td>
            <td>개발 중 내 PC에서 잠깐 확인</td>
        </tr>
    </tbody>
</table>

Minikube를 Docker 드라이버로 쓰는 macOS, Windows에서는 `minikube service --url` 이 내 PC와 클러스터 사이에 **임시 터널**을 열고 무작위 포트를 알려 준다. 터미널을 닫으면 터널도 닫히므로 접속하는 동안 띄워 둔다.

```bash
# Service 만들기
kubectl expose deployment nginx-app --type=NodePort --port=80
# 터널 열고 접속 URL 출력 (터미널 유지)
minikube service nginx-app --url

# 또는 localhost:8080 으로 바로 확인
kubectl port-forward deployment/nginx-app 8080:80
```

<br>
<hr>

## 7. NameSpace 

<br>

### 7.1 Namespace란

**Namespace는 한 클러스터 안의 리소스를 논리적으로 나누는 공간**이다. 팀, 프로젝트, 환경(dev, stage) 별로 리소스를 묶고, 같은 이름이 부딪히지 않게 한다. 서로 다른 Namespace에는 같은 이름의 Service를 만들 수 있다. 같은 아파트 단지에 101동 501호와 102동 501호가 함께 있는 것과 같다.

실습 리소스를 Namespace 하나에 모아 두면, 끝난 뒤 **Namespace만 지워 안의 리소스를 한 번에 정리**할 수 있다.

<table>
    <thead>
        <tr>
			<th>기능</th>
			<th>내용</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>리소스 분리</td>
            <td>팀, 프로젝트, 환경별로 나눠 관리</td>
        </tr>
        <tr>
            <td>접근 제어</td>
            <td>Role, RoleBinding으로 Namespace 별 권한 부여</td>
        </tr>
        <tr>
            <td>자원 제한</td>
            <td>ResourceQuota, LimitRange로 Namespace별 CPU, 메모리 제한</td>
        </tr>
        <tr>
            <td>설정 분리</td>
            <td>NetworkPolicy, ConfigMap, Secret을 Namespace 별로 관리</td>
        </tr>
    </tbody>
</table>

<br>

### 7.2 주의할 점

- **논리적인 구분일 뿐이다.** 기본 설정에서는 다른 NameSpace의 Service에서도 `서비스 이름.네임스페이스이름`으로 접속된다. 네트워크까지 막으려면 **NetworkPolicy**가 필요하다.
- Node, Persistence Volume, Namespace 자신처럼 **어느 Namespace에도 속하지 않는 (클러스터 범위)** 리소스도 있다.

<br>

### 7.3 기본으로 있는 Namespace

<table>
    <thead>
        <tr>
			<th>Namespace</th>
			<th>용도</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>default</td>
            <td>따로 지정하지 않으면 리소스가 들어가는 곳</td>
        </tr>
        <tr>
            <td>kube-system</td>
            <td>API 서버, 스케줄러, CoreDNS 같은 시스템 구성 요소</td>
        </tr>
        <tr>
            <td>kube-public</td>
            <td>인증하지 않은 사용자도 읽을 수 있는 공개 정보</td>
        </tr>
        <tr>
            <td>kube-node-lease</td>
            <td>노드가 살아 있음을 알리는 Lease 정보</td>
        </tr>
    </tbody>
</table>

```bash
kubectl get namespaces
kubectl create namespace practice
kubectl apply -f nginx-app.yaml -n practice
kubectl get all -n practice
# 기본 Namespace 바꾸기 (매번 -n 생략)
kubectl config set-context --current --namespace=practice
# Namespace 에 속하지 않은 리소스 목록
kubectl api-resources --namespace=false
# 안의 리소스까지 모두 삭제
kubectl delete namespace practice
```

<br>
<hr>

## 8. 용어 정리

<table>
    <thead>
        <tr>
			<th>용어</th>
			<th>한 줄 정의</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>Pod</td>
            <td>컨테이너를 하나 이상 묶은, 스케줄링되는 가장 작은 단위. 묶인 컨테이너는 네트워크와 스토리지를 공유</td>
        </tr>
        <tr>
            <td>Node</td>
            <td>Pod가 실행되는 물리 또는 가상 머신. kubelet, kube-proxy, 컨테이너 런타임이 돈다.</td>
        </tr>
        <tr>
            <td>ReplicaSet</td>
            <td>지정한 수의 Pod가 항상 떠 있도록 유지. 보통 Deployment가 만든다.</td>
        </tr>
        <tr>
            <td>Deployment</td>
            <td>ReplicaSet과 Pod를 선언적으로 관리. 롤아웃, 롤백 담당</td>
        </tr>
        <tr>
            <td>Service</td>
            <td>계속 바뀌는 Pod들 앞에 고정된 접근 지점(IP, DNS 이름)을 두고 트래픽을 나눈다.</td>
        </tr>
        <tr>
            <td>Namespace</td>
            <td>클러스터 안의 리소스를 나누는 논리적 공간</td>
        </tr>
        <tr>
            <td>Label / Selector</td>
            <td>리소스에 붙이는 키-값 꼬리표 / 그 꼬리표로 리소스를 고르는 조건</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

## 9. 핵심 정리

- Kubernetes의 관리 단위로 **Pod**다. Pod는 일회용이라 IP 대신 Service로, 데이터는 볼륨으로 다룬다.
- **컨테이너 재시작**은 같은 Pod 안의 일(IP 유지), **Pod 교체**는 새 Pod가 생기는 일(이름, IP 변경)이다.
- Phase는 Pending, Running, Succeeded, Failed, Unknown 다섯 가지다. **Running** 이라고 요청 받을 준비가 된 것은 아니다.
- 문제 Pod는 `get pods` → `describe (Events)` → `logs` → `logs --previous` 순서로 본다. 137은 OOMKilled, 143은 SIGTERM
- **Deployment → ReplicaSet → Pod** 순으로 만들어진다. ReplicaSet은 개수 유지, Deployment는 버전 교체와 롤백을 맡는다.
- Deployment는 Pod를 **라벨**로 찾는다. `selector`와 `template`의 라벨은 반드시 같아야 한다.
- Pod 네트워크는 클러스터 내부용이라, 밖에서 접근하려면 Service, Ingress, port-forward가 필요하다.
- Namespace는 **논리적** 구분이다. 네트워크 차단은 NetworkPolicy가 따로 필요하다.

<br>

