<br>

# Week 11 - Day 02. 회고

<br>

## 오늘 배운 것

- Kubernetes는 컨테이너를 항상 Pod로 감싸서 다루고, 스케줄링, 실행, 개수 관리의 대상이 모두 Pod라는 것을 배웠다.
- Pod의 Phase 다섯 가지와, `STATUS` 열에 자주 보이는 `ImagePullBackOff`, `CrashLoopBackOff`, `OOMKilled` 같은 값을 읽는 법을 배웠다.
- 종료 코드 137, 143을 "128 + 시그널 번호"로 읽는 법을 배웠다.
- Deployment가 ReplicaSet을 두고 Pod를 관리하는 구조와, 롤링 업데이트, 롤백, 스케일링, 자동 복구 기능을 배웠다.
- Deployment와 Service가 **라벨과 셀렉터**로 Pod를 찾는다는 것을 배웠다.
- `kubectl get deployment`의 READY, UP-TO-DATE, AVAILABLE이 각각 무엇을 묻는지 정리했다.
- Pod 네트워크는 클러스터 내부 전용이라, 밖에서 접근하려면 Service나 터널이 필요하다는 것을 배웠다.
- Namespace로 리소스를 나누고, 실습이 끝나면 Namespace째로 정리하는 방법을 배웠다.

<br>

## 헷갈렸던 점
### Running인데 왜 API 호출이 안 될까

처음에는 `Running`이면 애플리케이션이 다 뜬 줄 알았다. 그런데 `Running`은 **컨테이너 프로세스가 실행 중**이라는 뜻일 뿐이다. Spring Boot처럼 기동에 수십 초가 걸리는 앱은 프로세스가 떠 있어도 아직 포트를 열지 않거나 초기화 중일 수 있다. 그래서 요청을 받을 준비가 됐는지는 `READY` 열과 readinessProbe로 따로 본다. Phase와 "준비됨"을 구분해서 봐야 한다는 점이다.

### 컨테이너 재시작과 Pod 교체

**컨테이너 재시작은 Pod 안에서 일어나서 이름과 IP가 그대로**이고 `RESTARTS`만 올라간다. 반대로 **Pod 교체는 아예 새 Pod**라서 이름과 IP가 바뀐다. `CrashLoopBackOff` 가 계속 같은 Pod 이름으로 보이는 이유도 이것으로 설명된다. Pod는 그대로고 그 안의 컨테이너만 계속 재시작하는 중이기 때문이다.

### Deployment가 Pod를 직접 만들지 않은 이유

Deployment만 만들면 되는데 왜 중간에 ReplicaSet이 끼는지 처음에는 이해가 안 됐다. ReplicaSet은 **"지금 버전의 Pod를 N개 유지"** 하는 일만 하고, Deployment는 버전이 바뀔 때 **새 ReplicaSet을 만들어 Pod를 옮기고 옛 ReplicaSet은 기록으로 남긴다.** 역할이 나뉘어 있어서 롤백할 때 옛 ReplicaSet을 다시 키우기만 하면 된다. Pod 이름 가운데 해시가 ReplicaSet을 가리킨다는 것도 이렇게 연결됐다.

<br>

### 새롭게 알게 된 것

- **Deployment가 만든 Pod는 `Failed`가 되지 않는다.** `restartPolicy: Always`라서 실패해도 계속 재시작하고, 그 모습이 `CrashLoopBackOff`로 보인다.
- **`CrashLoopBackOff`의 대기 시간은 10초부터 두 배씩 늘어나 최대 5분**까지 간다. 재시작이 갑자기 느려져도 이상한 게 아니었다.
- **`kubectl logs --previous`** 로 직전에 죽은 컨테이너의 로그를 볼 수 있다. 재시작 직후에는 현재 컨테이너 로그가 비어 있어서 이걸 몰랐다면 원인을 놓쳤을 것이다.
- **`selector`와 `template`의 라벨이 다르면 생성 자체가 거부된다.** 실수로 조용히 넘기지 않고 처음부터 막아 준다.
- **`containerPort`는 적지 않아도 포트는 열린다.** 정보 제공용이지만 `kubectl expose`가 읽어가므로 적어 두는 편이 낫다.
- **`kubectl run`은 Pod 하나만 만든다.** 관리하는 컨트롤러가 없어서 지우면 끝이다.
- **Namespace는 네트워크까지 막아 주지 않는다.** 다른 Namespace의 Service에도 `이름.네임스페이스`로 접속되므로 격리가 필요하면 NetworkPolicy를 써야 한다.
- **Deployment를 지워도 Service가 남는다.** 별개 리소스라 따로 지워야 한다.

<br>

Pod는 일회용이고 **Deployment → ReplicaSet → Pod가 라벨로 이어져 개수와 버전을 맞춘다**는 구조를 잡고 나니, `Running`과 `READY`가 다른 이유, `CrashLoopBackOff`가 같은 Pod 이름으로 계속 보이는 이유까지 한 흐름으로 이해됐다.

<br>

