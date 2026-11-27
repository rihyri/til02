<br>

# Week 11 - Day 04. 회고

<br>

## 오늘 배운 것

- 컨테이너와 Pod가 언제든 사라질 수 있어서, 데이터를 지키려면 컨테이너 밖에 저장해야 한다는 것을 배웠다.
- emptyDir, hostPath, NFS가 **데이터를 어디에 두느냐**로 나뉘고, 그 위치가 데이터의 수명을 정한다는 것을 정리했다.
- 볼륨은 `volumes`(무슨 저장소)와 `volumeMounts`(어느 경로)로 나눠 적고 `name`으로 연결한다는 것을 배웠다.
- PV는 실제 저장소, PVC는 저장소 요청이고, Pod는 PVC만 알면 된다는 것을 배웠다.
- PV와 PVC가 바인딩되려면 StorageClass, 접근 모드, 용량 세 조건이 모두 맞아야 한다는 것을 배웠다.
- PV 라이프사이클(Available → Bound → Released)과 Retain, Delete, Recycle 반환 정책의 차이를 배웠다.
- PVC가 Terminating에서 멈추는 이유가 finalizer라는 보호 장치 때문이라는 것을 배웠다.
- StatefulSet이 고정 이름, Pod별 전용 PVC, 순서 있는 생성/삭제로 상태를 가진 앱을 다룬다는 것을 배웠다.
- Headless Service로 Pod마다 고정 DNS 이름을 만들고, Secret으로 DB 비밀번호를 분리하는 방법을 배웠다.

<br>

## 헷갈렸던 점

### hostPath의 "host"

hostPath라는 이름 때문에 내 맥북의 폴더가 마운트되는 줄 알았다. 실제로는 **Pod가 실행 중인 Kubernetes 노드**의 경로이다. Minikube에서는 노드가 Docker 컨테이너라서, 내 PC 경로를 적으면 노드 안에 그런 디렉터리가 없어 `type check failed`가 난다. 내 PC 폴더를 쓰고 싶으면 `minikube mount`로 먼저 노드에 연결해야 한다. Docker의 `-v` 바인드 마운트와 같은 감각으로 생각하면 안 된다.

### Retain인데 왜 다시 못 쓰나

Retain은 "데이터를 보존한다"는 뜻이니까 같은 이름으로 PVC를 다시 만들면 그대로 이어 쓸 수 있을 줄 알았다. 그런데 PV는 `Released` 상태로 이전 PVC 정보(`claimRef`)를 붙들고 있어서, 새 PVC는 이름이 같아도 **다른 요청**으로 취급되어 `Pending`에 머문다. 이전 사용자의 데이터가 다른 사람에게 넘어가지 않게 하려는 장치였다. **데이터 보존과 자동 재사용은 별개**이고, 다시 쓰려면 관리자가 데이터를 확인한 뒤 `claimRef`를 직접 지워야 한다.

### ReadWriteOnce는 Pod 하나만 쓰는 것인가
"Once"라서 Pod 하나만 쓸 수 있는 줄 알았다. 실제로는 **노드 하나** 기준이라, 같은 노드에 뜬 Pod 여러개는 함께 쓸 수 있다. Pod 하나로 확실히 제한하려면 ReadWriteOncePod를 써야 한다.

### PVC 삭제가 끝나지 않는 이유

`kubectl delete pvc`가 끝나지 않아서 처음엔 클러스터가 고장난 줄 알았다. 원인은 **아직 그 PVC를 쓰는 Pod가 있어서** finalizer가 삭제를 붙잡고 있는 것이었다. 고장이 아니라 보호 장치가 제대로 일한 것이다. 그리고 Pod가 StatefulSet 소속이면 Pod만 지워도 다시 생기니까, replicas를 0으로 줄이거나 StatefulSet을 먼저 지워야 한다. 결국 **쓰는 쪽부터 지운다(Pod → PVC → PV)** 는 원칙 하나로 정리된다.

### Deployment에도 PVC를 붙일 수 있는데 왜 StatefulSet인가

Deployment에 PVC를 붙이면 모든 replica가 **같은 PVC 하나**를 나눠 쓴다. RWO 디스크라면 다른 노드의 Pod는 동시에 마운트할 수 없다. DB 복제본처럼 **각자 자기 데이터 디렉터리**가 필요한 경우에는 `volumeClaimTemplates` 로 Pod마다 PVC를 따로 만들어 주는 StatefulSet이 필요하다는 것을 이해했다.

<br>

## 새롭게 알게 된 것

- **볼륨을 마운트 하면 그 경로의 기존 파일이 가려진다.** 빈 PVC를 nginx html 경로에 붙이면 기본 `index.html` 도 안 보여서 403이 난다. 에러 원인이 설정 문제가 아니라 "비어 있어서" 일 수 있다.
- **emptyDir을 `medium: Memory`로 쓰면 그 데이터가 컨테이너 메모리 사용량에 잡힌다.** 큰 파일을 쓰면 OOMKilled가 날 수 있다.
- **PV는 네임스페이스가 없는 클러스터 리소스다.** 그래서 `kubectl get pv`에 `-n`을 붙일 필요가 없다.
- **PVC는 맞는 PV가 없으면 실패하지 않고 기다린다.** 나중에 PV가 생기면 알아서 바인딩된다.
- **정적 프로비저닝에서 큰 PV가 통째로 묶인다.** 500Mi를 원해도 1Gi PV가 묶이면 나머지 500Mi는 아무도 못 쓴다.
- **`volumeBindingMode: waitForFirstConsumer`가 필요한 이유는 가용 영역이다.** EBS는 같은 영역의 노드에만 붙으니, Pod 위치가 정해진 뒤에 디스크를 만들어야 한다.
- **이미 만든 PV의 반환 정책도 `kubectl patch`로 바꿀 수 있다.** 동적으로 만든 중요한 DB 볼륨을 Retain으로 돌려 둘 수 있다.
- **Headless Service는 nslookup 결과로 Pod IP를 전부 돌려준다.** 일반 Service는 가상 IP 하나만 준다.
- **StatefulSet을 지우거나 스케일 다운해도 PVC는 남는다.** 다시 늘리면 같은 번호의 Pod에 같은 PVC가 붙는다. 대신 실습 뒤에는 PVC를 직접 정리해야 디스크가 쌓이지 않는다.
- **Secret의 base64는 `base64 -d` 한 줄로 풀린다.** Secret은 "숨긴 것"이 아니라 "설정과 분리한 것"에 가깝다.

<br>

볼륨은 **어디에 두느냐,** PV/PVC는 **누가 준비하고 누가 쓰느냐,** StatefulSet은 **누구인지 기억하느냐**의 문제였고, Terminating과 Released처럼 "안 되는 것처럼 보이는 상태"가 사실은 데이터를 지키려는 보호 장치라는 점이 가장 인상 깊었다.

<br>

