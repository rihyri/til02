<br>

# Week 11 - Day 04. 볼륨과 statefulSet

<br>

이번 수업은 **컨테이너 데이터가 사라지는 문제 → Pod 단위로 붙이는 볼륨 → 저장소를 준비하는 쪽과 쓰는 쪽을 나눈 PV/PVC → 상태를 가진 앱을 위한 StatefulSet** 순서로 진행됐다. 그 중 **PV/PVC 라이프 사이클과 반환 정책,** 그리고 **StatefulSet이 Deployment와 다른 세 가지(고정 이름, 전용 저장소, 순서)** 가 특히 중요하다.

<br>
<hr>

## 1. 컨테이너 데이터는 왜 사라지나

컨테이너 안에 쓴 파일은 컨테이너가 지워지면 함께 사라진다. Kubernetes에서 여기에 더해 **Pod 자체가 언제든 삭제되고 다른 노드에서 새로 만들어질 수 있다.** 그래서 데이터를 지키려면 컨테이너 바깥, 경우에 따라서는 노드 바깥에 저장해야 한다.

<br>
<hr>

## 2. 볼륨

<br>

### 2.1 볼륨이란

Pod에 붙는 저장 공간이다. Pod 안의 컨테이너들이 원하는 경로에 마운트해서 쓴다. 컨테이너가 재시작되어도 볼륨의 데이터는 남고, 같은 Pod의 컨테이너끼리 데이터를 나눠 쓸 수도 있다.

볼륨은 Pod 명세의 **두 곳**에 적고, `name`으로 둘을 연결한다.

<table>
    <thead>
        <tr>
			<th>위치</th>
			<th>정하는 것</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>spec.volumes</td>
            <td>어떤 저장소를 쓸지 (empty Dir, hostPath, nfs, persistenceVolumeClaim, ConfigMap, secret 등)</td>
        </tr>
        <tr>
            <td>containers[].volumeMounts</td>
            <td>그 저장소를 컨테이너의 어느 경로에 붙일지</td>
        </tr>
    </tbody>
</table>

```yaml
spec:
  containers:
    - name: app
      image: nginx:1.27
      volumeMounts:
        - name: html                #  아래 volumes 의 name 과 같아야 한다.
          mountPath: /usr/share/nginx/html
          readOnly: true
  volumes:
    - name: html
      persistentVolumeClaim:
        claimName: my-pvc
```

- 하나의 볼륨을 여러 컨테이너가 **서로 다른 경로**로 마운트할 수 있다.
- `subPath`를 쓰면 볼륨 안의 특정 하위 경로만 마운트한다.
- **마운트한 경로의 기존 파일은 가려진다.** 위 예시에서 PVC가 비어 있으면 nginx 기본 `index.html` 도 보이지 않아 403이 난다.

<br>

### 2.2 볼륨 종류 ⭐

세 볼륨은 **데이터를 어디에 두느냐**가 다르고, 그 위치가 데이터의 수명을 정한다.

<table>
    <thead>
        <tr>
			<th>종류</th>
			<th>데이터 위치</th>
			<th>주 용도</th>
			<th>주의</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>emptyDir</td>
            <td>Pod (노드 디스크 또는 메모리)</td>
            <td>같은 Pod 컨테이너끼리 파일 공유(사이드카), 캐시, 임시 파일</td>
            <td>Pod가 지워지면 함께 삭제</td>
        </tr>
        <tr>
            <td>hostPath</td>
            <td>Pod가 실행 중인 노드의 파일 시스템</td>
            <td>노드 로그 수집, 모니터링 에이전트 같은 시스템 Pod</td>
            <td>다른 노드로 옮겨지면 이전 데이터를 못 봄, 보안 위험</td>
        </tr>
        <tr>
            <td>NFS</td>
            <td>클러스터 밖의 파일 서버 (NFS, NAS, EFS)</td>
            <td>여러 Pod가 함께 쓰는 업로드 파일, 정적 리소스</td>
            <td>네트워크라 느리고, 동시 쓰기 잠금 문제로 DB 저장소에는 부적합</td>
        </tr>
    </tbody>
</table>

<table>
    <thead>
        <tr>
			<th>상황</th>
			<th>컨테이너 파일시스템</th>
			<th>emptyDir</th>
			<th>hostPath</th>
            <th>NFS / PV</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>컨테이너 재시작</td>
            <td>사라짐</td>
            <td>유지</td>
            <td>유지</td>   
            <td>유지</td>
        </tr>
        <tr>
            <td>Pod 삭제</td>
            <td>사라짐</td>
            <td>사라짐</td>
            <td>노드에 남음</td>
            <td>유지</td>
        </tr>
        <tr>
            <td>다른 노드로 이동</td>
            <td>사라짐</td>
            <td>사라짐</td>
            <td>이전 노드에만 있음</td>   
            <td>다시 연결됨</td>
        </tr>
    </tbody>
</table>

<br>

#### · emptyDir - 사이드카로 로그 공유

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: emptydir-pod
spec:
  containers:
    - name: writer
      image: busybox:1.36
      command: ["/bin/sh", "-c", "while true; do date >> /shared/app.log; sleep 1; done"],
      volumeMounts:
        - name: shared
          mountPath: /shared
    - name: reader
      image: busybox:1.36
      command: ["/bin/sh", "-c", "tail -F /shared/app.log"]
      volumeMounts:
        - name: shared
          mountPath: /shared   
  volumes:
    - name: shared
      emptyDir: {}            # medium : Memory 로 바꾸면 tmpfs 사용
```

`medium: Memory`를 쓰면 더 빠르지만, 쓴 데이터가 **컨테이너 메모리 사용량에 포함**되므로 메모리 limit을 함께 고려해야 한다.

```bash
kubectl apply -f emptydir-pod.yaml
kubectl logs -f emptydir-pod -c reader        # writer 가 쓴 내용이 reader 에 보이는지
kubectl exec emptydir-pod -c writer -- ls /shared
```

<br>

#### · hostPath - "host"는 내 PC가 아니라 노드다.
Minikube의 노드는 Docker 컨테이너나 VM이다. 그래서 macOS의 `/private/tmp` 같은 경로를 적으면 노드 안에 그 디렉터리가 없어 `hostPath type check failed`로 pod가 뜨지 않는다.

<table>
    <thead>
        <tr>
			<th>type</th>
			<th>의미</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>DirectoryOrCreate</td>
            <td>디렉터리가 없으면 만든다.</td>
        </tr>
        <tr>
            <td>Directory</td>
            <td>이미 있어야 한다.</td>
        </tr>
        <tr>
            <td>FileOrCreate / File</td>
            <td>파일 (없으면 생성 / 있어야 함)</td>
        </tr>
        <tr>
            <td>(생략)</td>
            <td>검사하지 않음</td>
        </tr>
    </tbody>
</table>

```bash
# 노드 안에서 파일 확인
minikube ssh -- cat /data/hostpath-demo/hostpath.log

# 내 PC 폴더를 노드에 연결 (터미널 유지)
minikube mount $HOME/k8s-data:/data/host-share
```

<br>
<hr>

## 3. PV와 PVC

<br>

### 3.1 왜 나눴나

일반 볼륨은 Pod 명세에 **NFS 서버 주소 같은 저장소 세부 정보**를 직접 적어야 한다. PV/PVC는 이것을 두 역할로 나눈다.

<table>
    <thead>
        <tr>
			<th>리소스</th>
			<th>누가</th>
			<th>범위</th>
			<th>역할</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>PV (Persistent Volume)</td>
            <td>관리자 또는 StorageClass</td>
            <td>클러스터(네임 스페이스 없음)</td>
            <td>실제 스토리지를 등록한 것</td>
        </tr>
        <tr>
            <td>PVC (Persistent VolumeClaim)</td>
            <td>개발자</td>
            <td>네임스페이스</td>
            <td>"1Gi, RWO 짜리 저장소가 필요하다"는 요청</td>
        </tr>
        <tr>
            <td>StorageClass</td>
            <td>관리자</td>
            <td>클러스터</td>
            <td>PVC가 들어오면 PV를 자동으로 만들어 주는 프로비저너</td>
        </tr>
    </tbody>
</table>

Pod는 PV를 직접 모르고 **PVC 이름만** 안다. 저장소가 NFS에서 클라우드 디스크로 바뀌어도 앱의 YAML은 그대로다. 호텔에 "2인실 하나"라고 예약(PVC)하면 실제 몇 호실(PV)인지는 프런트가 정해주는 것과 같다.

<br>

### 3.2 바인딩 조건

PVC와 PV는 1:1로 묶이고, 묶인 PV는 다른 PVC가 쓸 수 없다. 아래 세 조건이 **모두** 맞아야 바인딩된다.

<table>
    <thead>
        <tr>
			<th>항목</th>
			<th>조건</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>storageClassName</td>
            <td>PV와 PVC가 같아야 한다.</td>
        </tr>
        <tr>
            <td>accessModes</td>
            <td>PV가 PVC의 요청 모드를 지원해야 한다.</td>
        </tr>
        <tr>
            <td>capacity</td>
            <td>PV 용량 ≥ PVC 요청량 (500Mi 요청에 1Gi PV가 묶이면 1Gi가 통째로 할당)</td>
        </tr>
    </tbody>
</table>

맞는 PV가 없으면 PVC는 **실패가 아니라 `pending`으로 대기**하다가, 조건에 맞는 PV가 생기는 순간 자동으로 바인딩된다.

<br>

### 3.3 접근 모드

<table>
    <thead>
        <tr>
			<th>모드</th>
			<th>약어</th>
			<th>의미</th>
			<th>예</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>ReadWriteOnce</td>
            <td>RWO</td>
            <td>한 노드에서 읽기/쓰기</td>
            <td>EBS, hostPath</td>
        </tr>
        <tr>
            <td>ReadOnlyMany</td>
            <td>ROX</td>
            <td>여러 노드에서 읽기 전용</td>
            <td>공유 설정 파일</td>
        </tr>
        <tr>
            <td>ReadWriteMany</td>
            <td>RWX</td>
            <td>여러 노드에서 읽기/쓰기</td>
            <td>NFS, EFS</td>
        </tr>
        <tr>
            <td>ReadWriteOncePod</td>
            <td>RWOP</td>
            <td>한 Pod만 읽기/쓰기</td>
            <td>단독 사용이 필요한 DB</td>
        </tr>
    </tbody>
</table>

RWO는 "Pod 하나"가 아니라 "**노드** 하나"다. 같은 노드의 Pod 여러 개는 함께 쓸 수 있다. Pod 하나로 제한하려면 RWOP를 쓴다.

<br>

### 3.4  라이프사이클 ⭐

```aiignore
프로비저닝 → 바인딩 → 사용 중 → 반환

PV 상태:  Available ──바인딩──→ Bound ──PVC 삭제──→ Released ─┬─ Delete 정책 → PV, 실제 디스크 삭제
              ▲                                                └─ Retain 정책 → 데이터 보존
              └──────────── 관리자가 claimRef 를 지우면 ─────────────┘
```

<br>

**· 프로비저닝 - 정적과 동적**

<table>
    <thead>
        <tr>
			<th>구분</th>
			<th>정적</th>
			<th>동적</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>PV 생성</td>
            <td>관리자가 미리</td>
            <td>PVC가 들어오면 StorageClass가 자동으로</td>
        </tr>
        <tr>
            <td>크기</td>
            <td>미리 만든 크기 (요청보다 클 수 있음)</td>
            <td>요청한 크기 그대로</td>
        </tr>
        <tr>
            <td>기본 반환 정책</td>
            <td>직접 지정</td>
            <td>StorageClass의 reclaimPolicy (기본 Delete)</td>
        </tr>
        <tr>
            <td>운영 환경</td>
            <td>거의 안 씀</td>
            <td>대부분 (EKS, GKE, AKS 기본 제공)</td>
        </tr>
    </tbody>
</table>

- Minikube에는 기본 StorageClass `standard`가 있어서 PVC만 만들면 노드에 디렉터리가 자동으로 생긴다.
- PVC에 `storageClassName: ""`를 적으면 동적 프로비저닝을 끄고, StorageClass가 없는 PV와만 바인딩한다.

<br> 

#### · 사용 중 - 삭제 보호

Pod가 쓰고 있는 PVC, PVC에 묶인 PV는 삭제 요청을 받아도 바로 지워지지 않고 `Terminating`으로 기다린다. (Storage Object in Use Protection)

```bash
# 기본 StorageClass 확인 (default) 표시
kubectl get storageclass

# STATUS: Available / Bound / Released 
kubectl get pv

# STATUS: Pending / Bound
kubectl get pvc 

# Pending 이면 Events 에 이유가 나온다.
kubectl describe pvc my-pvc
```

<br>

### 3.5 반환 정책 ⭐

PVC가 지워진 뒤 PV를 어떻게 할지 `persistentVolumeReclaimPolicy` 로 정한다.

<table>
    <thead>
        <tr>
			<th>정책</th>
			<th>PVC 삭제 후</th>
			<th>데이터</th>
			<th>쓰는 곳</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>Retain</td>
            <td>PV가 Released로 남음</td>
            <td>보존</td>
            <td>DB처럼 잃으면 안 되는 데이터</td>
        </tr>
        <tr>
            <td>Delete</td>
            <td>PV와 실제 스토리지까지 삭제</td>
            <td>삭제</td>
            <td>캐시, 개발/테스트 데이터, 동적 프로비저닝 기본값</td>
        </tr>
        <tr>
            <td>Recycle</td>
            <td>데이터를 지우고 다시 Available</td>
            <td>삭제</td>
            <td>Deprecated. 대신 동적 프로비저닝이나 Retain 후 수동 처리</td>
        </tr>
    </tbody>
</table>

**Retain으로 남은 PV는 새 PVC와 자동으로 묶이지 않는다.** PV에 이전 PVC정보(`claimRef`)가 남아 있어서, 같은 이름으로 PVC를 다시 만들어도 Kubernetes는 다른 요청으로 보고 `Pending`으로 둔다. 이전 사용자의 데이터가 다른 사람에게 넘어가지 않게 막는 장치다.

```bash
# Released 된 PV 를 다시 쓰려면: 데이터를 비울지 먼저 판단한 뒤 claimRef 제거
kubectl get pv my-retain-pv -o jsonpath='{.spec.claimRef.name}'; echo
kubectl patch pv my-retain-pv -p '{"spec": {"claimRef":null}}'
kubectl get pv my-retain-pv           # Released → Available
```

동적 프로비저닝 예시 (AWS EBS):

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gp3
provisioner: ebs.cis.aws.com
reclaimPolicy: Delete                     # Retain 으로 바꾸면 디스크를 남긴다.
volumeBindingMode: waitForFirstConsumer   # Pod 가 배치될 노드(가용 영역)가 정해진 뒤 디스크 생성
parameters:
  type: gp3
```

`waitForFirstConsumer` 를 쓰는 이유는 EBS가 **같은 가용 영역의 노드에만 붙기** 때문이다. PVC를 만들자마자 디스크를 만들면 Pod가 다른 영역에 배치되어 붙지 못할 수 있다.

이미 만들어진 PV의 정책은 나중에 바꿀 수도 있다.

```bash
kubectl patch pv <pv-name> -p '{"spec":{"persistentVolumeReclaimPolicy":"Retain"}}'
```

<br>

## 4. PVC가 Termination에서 안 지워질 때

<br>

대부분 **아직 그 PVC를 쓰는 Pod가 있기 때문이다.** PVC에는 `kubernetes.io/pvc-protection` **finalizer**가 붙어 있고, finalizer는 "이 조건이 풀리기 전에 삭제를 끝내지 말라"는 표시다. 즉 Terminating은 고장이 아니라 **보호 장치가 정상 동작 중**이라는 뜻인 경우가 많다.

그래서 지울 때 **사용하는 쪽부터 - Pod → PVC → PV** 순서로 지운다.

<table>
    <thead>
        <tr>
			<th>단계</th>
			<th>확인할 것</th>
			<th>명령</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>1</td>
            <td>PVC를 쓰는 Pod가 있는가</td>
            <td>kubectl describe pvc <pvc> | grep "Used By"</td>
        </tr>
        <tr>
            <td>2</td>
            <td>그 Pod를 Deployment/StatefulSet이 관리하는가</td>
            <td>관리 중이면 Pod를 지워도 다시 생기므로 scale --replicas = 0 또는 컨트롤러 삭제</td>
        </tr>
        <tr>
            <td>3</td>
            <td>상태와 이벤트</td>
            <td>kubectl describe pvc <pvc></td>
        </tr>
        <tr>
            <td>4</td>
            <td>남은 finalizer</td>
            <td>kubectl get pvc <pvc> -o jsonpath='{.metadata.finalizers}'</td>
        </tr>
        <tr>
            <td>5</td>
            <td>(최후의 수단) finalizer 강제 제거</td>
            <td>kubectl patch pvc <pvc> -p '{"metadata": {"finalizers":null }}'</td>
        </tr>
    </tbody>
</table>

```bash
kubectl describe pvc my-pvc | grep "Used By"
kubectl scale statefulset web --replicas=0       # 컨트롤러가 관리하는 Pod 라면
kubectl delete pod <pod-name>                    # 직접 만든 Pod 라면
kubectl get pvc my-pvc -w                        # Pod 가 사라지면 자동으로 삭제 완료
```

finalizer를 강제로 지우면 **Pod가 아직 볼륨을 쓰는 중에도 삭제가 진행**되어 데이터가 깨질 수 있다. 반드시 1~2단계를 먼저 확인한다.

<br>
<hr>

## 5. StatefulSet

<br>

### 5.1 왜 필요한가

Deployment는 **모든 Pod가 똑같아서 아무거나 바꿔 끼워도 되는 (stateless)** 앱을 위한 것이다. 반면 MySQL, Redis, Kafka, ElasticSearch 같은 상태를 가진 앱은 다음이 필요하다.

- **고정된 이름과 주소** - "0번이 리더, 1·2번이 복제본"처럼 서로를 구분하고 찾아야 한다.
- **인스턴스별 전용 저장소** - 복제본끼리 같은 데이터 파일을 공유하면 안 된다.
- **순서 있는 시작과 종료** - 리더가 먼저 뜨고 복제본이 나중에 붙어야 한다.

<table>
    <thead>
        <tr>
			<th>구분</th>
			<th>Deployment</th>
			<th>StatefulSet</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>Pod 이름</td>
            <td>web-7c5ddbdf54-x8k2p (무작위)</td>
            <td>web-0, web-1, web-2 (고정 순번)</td>
        </tr>
        <tr>
            <td>재생성 시</td>
            <td>새 이름</td>
            <td>같은 이름</td>
        </tr>
        <tr>
            <td>저장소</td>
            <td>PVC 하나를 공유하거나 없음</td>
            <td>volumeClaimTemplates 로 Pod마다 전용 PVC</td>
        </tr>
        <tr>
            <td>네트워크</td>
            <td>Service 하나로만 접근</td>
            <td>Headless Service로 Pod마다 고정 DNS</td>
        </tr>
        <tr>
            <td>생성/삭제 순서</td>
            <td>동시에</td>
            <td>0 → 1 → 2 순서로 생성, 역순으로 삭제 (기본)</td>
        </tr>
        <tr>
            <td>용도</td>
            <td>웹/API 서버</td>
            <td>DB, 메시지 브로커, 분산 저장소</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

## 5.2 매니페스트

StatefulSet은 보통 **Headless Service와 한 쌍**으로 만든다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx
spec:
  clusterIP: None            # Headless: 가상 IP 없이 Pod 별 DNS 를 만든다
  selector:
    app: web
  ports:
    - port: 80
      name: http
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: web
spec:
  serviceName: nginx         # 위 Headless Service 이름
  replicas: 3
  selector:
    matchLabels:
      app: web
  template:
    metadata:
      labels:
        app: web
    spec:
      containers:
        - name: nginx
          image: nginx:1.27
          volumeMounts:
            - name: www
              mountPath: /usr/share/nginx/html
  volumeClaimTemplates:      # Pod 마다 PVC 생성: www-web-0, www-web-1, www-web-2
    - metadata:
        name: www
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 100Mi
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
            <td>clusterIP: None</td>
            <td>일반 Service는 nslookup 결과로 가상 IP 하나를 주지만, Headless는 Pod IP 전부를 준다. web-0.nginx 처럼 Pod 이름을 붙이면 특정 Pod 하나에 바로 접속한다.</td>
        </tr>
        <tr>
            <td>ServiceName</td>
            <td>어떤 Headless Service로 DNS를 만들지 지정</td>
        </tr>
        <tr>
            <td>volumeClaimTemplates</td>
            <td>PVC의 틀. PVC 이름은 <템플릿명>-<Pod명>. Pod가 같은 이름으로 다시 생기면 같은 PVC가 다시 붙는다.</td>
        </tr>
    </tbody>
</table>

Pod DNS 전체 이름은 `<pod>.<service>.<namespace>.svc.cluster.local` 이다. Pod가 다시 만들어져 IP가 바뀌어도 이 이름은 그대로다.

<br>

## 5.3 실습 명령어

```bash
kubectl apply -f week-11/day-04/statefulset/web-statefulset.yaml
kubectl get pods -w -l app=web                # web-0 → web-1 → web-2 순서로 생성        
kubectl get pvc                               # www-web-0, www-web-1, www-web-2

# Pod 마다 다른 내용 쓰기 (쓰기 전에는 빈 PVC 가 html 경로를 가려 403)
for i in 0 1 2; do
  kubectl exec web-$i -- sh -c "echo \$(hostname) > /usr/share/nginx/html/index.html"
done

# Headless DNS 확인: 가상 IP 대신 Pod IP 3개가 나온다.
kubectl run tmp --rm -it --image=busybox:1.36 --restart=Never -- nslookup nginx
kubectl run tmp --rm -it --image=busybox:1.36 --restart=Never -- wget -qO- http://web-1.nginx

# Pod 를 지워도 같은 이름, 같은 데이터로 돌아오는지
kubectl delete pod web-1
kubectl get pods -w -l app=web
kubectl exec web-1 -- cat /usr/share/nginx/html/index.html    # 여전히 web-1

# 스케일 다운은 큰 번호부터, PVC 는 남는다.
kubectl scale statefulset web --replicas=1
kubectl get pods, pvc
```

**StatefulSet을 지우거나 줄여도 PVC는 기본적으로 남는다.** 데이터를 실수로 날리지 않기 위해서다. 다시 늘리면 남아 있던 PVC가 같은 번호의 Pod에 다시 붙는다. 실습이 끝나면 PVC를 직접 지워야 한다.

```bash
kubectl delete -f week-11/day-04/statefulset/web-statefulset.yaml
kubectl delete pvc -l app=web                 # 템플릿 PVC 에는 Pod 라벨이 붙는다.
```

> 순서가 필요 없는 경우 `podManagementPolicy: Parallel` 로 Pod를 동시에 만들고 지울 수 있다. 기본 값은 `OrderedReady` 다.

<br>

### 5.4 Secret과 함께 MySQL 실행하기

```yaml
apiVersion: v1
kind: secret
metadata:
  name: mysql-secret
type: Opaque
stringData:         # 평문으로 적으면 저장 시 base64 로 바뀐다.
  MYSQL_ROOT_PASSWORD: password
  MYSQL_DATABASE: memo

---

apiVersion: v1
kind: Service
metadata:
  name: mysql
spec:
  clusterIP: None
  selector:
    app: mysql
  ports:
    - port: 3306
      
---

apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql
spec:
  serviceName: mysql
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
        - name: mysql
          image: mysql:8.0
          envFrom:
            - secretRef:
                name: mysql-secret        # Secret 의 모든 키를 환경 변수로
          ports:
            - containerPort: 3306
          readinessProbe:
            exec:
              command: ["sh", "-c", "mysqladmin ping -h 127.0.0.1 -uroot -p\"$MYSQL_ROOT_PASSWORD\""]
              initialDelaySeconds: 15
              periodSeconds: 5
            volumeMounts:
              - name: data
                mountPath: /var/lib/mysql
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 1Gi
```

- MySQL 공식 이미지는 `MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE`를 읽어 **첫 실행 때** 비밀번호와 DB를 만든다.
- 첫 실행은 데이터 디렉터리 초기화에 수십 초가 걸리므로, `mysqladmin ping`이 성공할 때까지 READY가 `0/1`이다.
- `/var/lib/mysql`이 전용 PVC `data-mysql-0`에 있으므로 Pod를 지워도 데이터가 남는다.
- Spring Boot 접속 주소는 `jdbc:mysql://mysql-0.mysql:3306/memo` 처럼 Pod DNS를 쓴다.

#### · Secret 주의점 

- base64는 **인코딩일 뿐 암호화가 아니다.** 누구나 되돌릴 수 있다.
- 실제 프로젝트에서는 Secret 매니페스트를 Git에 올리지 않고, 명령을 만들거나 Sealed Secrets, External Secrets 같은 도구를 쓴다.

```bash
kubectl create secret generic mysql-secret \
  --from-literal=MYSQL_ROOT_PASSWORD='<비밀번호>' \
  --from-literal=MYSQL_DATABASE=memo

kubectl get secret mysql-secret -o jsonpath='{.data.MYSQL_DATABASE}' | base64 -d; echo   # 바로 복원됨

# 데이터가 남는지 확인
kubectl exec -it mysql-0 -- sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" memo -e "CREATE TABLE t(id INT); INSERT INTO t VALUES (1);"'
kubectl delete pod mysql-0
kubectl wait --for=condition=Ready pod/mysql-0 --timeout=120s
kubectl exec -it mysql-0 -- sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" memo -e "SELECT * FROM t;"'
```

<br>
<hr>

## 6. 핵심 정리

- 볼륨은 **데이터 위치가 수명을 정한다.** emptyDir은 Pod와 함께, hostPath는 노드에, NFS/PV는 Pod와 노드에 상관없이 남는다.
- hostPath의 host는 **내 PC가 아니라 노드**다.
- 마운트한 경로의 기존 파일은 가려진다. 빈 PVC를 nginx html 경로에 붙이면 403이 난다.
- **PV는 클러스터 범위의 실제 저장소, PVC는 네임스페이스 범위의 요청.** Pod는 PVC만 안다.
- 바인딩 조건은 **StorageClass, 접근 모드, 용량** 세 가지. 안 맞으면 Pending으로 대기한다.
- RWO는 한 **노드**, RWOP는 한 **Pod**다.
- **Retain → Released로 보존(claimRef 정리 필요), Delete → 실제 디스크까지 삭제, Recycle → 쓰지 않음.** 운영은 동적 프로비저닝이 표준이다.
- PVC가 Terminating이면 **먼저 쓰는 Pod를 찾는다.** finalizer 강제 제거는 최후의 수단이다.
- StatefulSet = **고정 이름 + Pod별 전용 PVC + 순서,** 그리고 Headless Service로 Pod마다 고정 DNS를 준다.
- StatefulSet을 지워도 **PVC는 남는다.**
- Secret의 base64는 암호화가 아니다. Secret 매니페스트는 Git에 올리지 않는다.

<br>

