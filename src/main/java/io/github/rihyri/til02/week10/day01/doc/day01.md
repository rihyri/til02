<br>

# Week 10 - Day 01. 클라우드와 Docker

<br>

이번 수업은 **클라우드가 무엇이고 왜 쓰는지 → 그 위에 애플리케이션을 어떤 단위로 올리는지(물리 서버 → VM → 컨테이너) → 컨테이너를 다루는 도구 Docker → 여러 컨테이너를 묶은 Docker Compose** 순서로 진행됐다.

<br>
<hr>

## 1. 클라우드 컴퓨팅

<br>

### 1.1 정의

**클라우드 컴퓨팅은 서버, 스토리지, 네트워크 같은 IT 자원을 인터넷으로 필요한 만큼 빌려 쓰고, 쓴 만큼 돈을 내는 방식이다.**

내 컴퓨터는 요청을 보내고 결과를 받는 역할만 한다. 실제 연산과 저장은 클라우드 사업자의 데이터센터에서 일어난다.

미국 표준기술연구소(NIST)는 클라우드의 필수 특성을 다섯 가지로 정의한다. 아래 특성을 갖춰야 "클라우드"라고 부를 수 있다는 기준이다.

<table>
    <thead>
        <th>특성</th>
        <th>의미</th>
    </thead>
    <tbody>
        <tr>
            <td>온디맨드 셀프서비스</td>
            <td>사업자 직원을 거치지 않고 사용자가 직접 자원을 만들고 지운다.</td>
        </tr>
        <tr>
            <td>폭넓은 네트워크 접근</td>
            <td>네트워크만 있으면 어떤 단말에서도 접근할 수 있다.</td>
        </tr>
        <tr>
            <td>자원 풀링</td>
            <td>여러 고객이 물리 자원을 나눠 쓰고, 사용자는 그 자원이 정확히 어디 있는지 몰라도 된다.</td>
        </tr>
        <tr>
            <td>빠른 탄력성</td>
            <td>수요에 따라 자원을 빠르게 늘리고 줄인다.</td>
        </tr>
        <tr>
            <td>사용량 측정</td>
            <td>쓴 만큼 측정되고, 측정된 만큼 과금된다.</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

### 1.2 왜 쓰는가

<table>
    <thead>
        <th>이유</th>
        <th>설명</th>
    </thead>
    <tbody>
        <tr>
            <td>비용 구조 변화</td>
            <td>서버를 미리 사는 초기 투자(CAPEX)가 쓴 만큼 내는 운영비(OPEX)로 바뀐다. 스타트업도 서버 한 대로 시작할 수 있다.</td>
        </tr>
        <tr>
            <td>속도</td>
            <td>서버를 주문하고 설치하는 데 몇 주가 걸리던 일이 몇 분으로 줄어든다.</td>
        </tr>
        <tr>
            <td>탄력성</td>
            <td>트래픽이 몰리면 늘리고, 줄면 다시 줄인다. 최대 트래픽 기준으로 서버를 사 둘 필요가 없다.</td>
        </tr>
        <tr>
            <td>운영 부담 감소</td>
            <td>하드웨어 교체, OS 패치 같은 일을 사업자가 맡는다.</td>
        </tr>
        <tr>
            <td>안정성과 글로벌 배포</td>
            <td>여러 리전(Region)과 가용 영역(AZ)에 나눠 배치하면 데이터센터 하나에 장애가 나도 서비스가 유지된다. 해외 사용자 근처에 서버를 두어 응답 속도를 높일 수 있다.</td>
        </tr>
    </tbody>
</table>

<br>

## 1.3 서비스 모델 : 어디까지 빌리는가

세 모델을 구분하는 기준은 **"사업자가 어디까지 관리해 주는가"** 다. 아래로 갈수록 사용자가 신경쓸 범위가 줄어든다.

<table>
    <thead>
        <th>모델</th>
        <th>빌리는 것</th>
        <th>사용자가 관리하는 것</th>
        <th>예시</th>
    </thead>
    <tbody>
        <tr>
            <td>IaaS</td>
            <td>가상 서버, 네트워크, 스토리지</td>
            <td>OS, 런타임, 애플리케이션, 데이터</td>
            <td>AWS EC2, GCP Compute Engine</td>
        </tr>
        <tr>
            <td>PaaS</td>
            <td>애플리케이션 실행 플랫폼</td>
            <td>애플리케이션 코드와 데이터</td>
            <td>AWS Elastic Beanstalk, Google App Engine</td>
        </tr>
        <tr>
            <td>SaaS</td>
            <td>완성된 소프트웨어</td>
            <td>사용(설정, 데이터 입력) 정도</td>
            <td>Gmail, Slack, Notion</td>
        </tr>
    </tbody>
</table>

피자로 비유하자면 이렇다. IaaS는 주방과 오븐을 빌려 직접 반죽부터 만드는 것, PaaS는 도우와 토핑까지 받아 굽기만 하는 것, SaaS는 완성된 피자를 배달받는 것이다.

<br>

### 1.4 배포 형태 : 누구의 인프라인가

<table>
    <thead>
        <th>형태</th>
        <th>설명</th>
        <th>특징</th>
    </thead>
    <tbody>
        <tr>
            <td>Private Cloud</td>
            <td>한 조직만 쓰는 전용 클라우드. 다른 조직과 공유하지 않는다.</td>
            <td>보안과 구성을 조직 입맛대로 맞출 수 있고 성능이 예측 가능하다. 대신 비용과 운영 부담이 크다.</td>
        </tr>
        <tr>
            <td>Public Cloud</td>
            <td>사업자가 소유한 인프라를 여러 고객이 나눠쓴다.</td>
            <td>초기 비용이 없고 확장이 쉽다. 일반적으로 말하는 AWS, GCP, Azure</td>
        </tr>
        <tr>
            <td>Hybrid Cloud</td>
            <td>Private와 Public을 네트워크( VPN, 전용선, API 등)로 연결해 하나처럼 쓴다.</td>
            <td>민감한 데이터는 Private에, 트래픽 변동이 큰 서비스는 Public에 두는 식으로 나눈다.</td>
        </tr>
    </tbody>
</table>

NIST 정의에는 여기에 **Community Cloud**(관심사가 같은 여러 조직이 함께 쓰이는 형태)가 하나 더 있다.

<br>
<hr>

## 2. 애플리케이션 배포 방식의 변화

클라우드 위에 무엇을 어떤 단위로 올리느냐도 바뀌어 왔다. 이 흐름을 알면 Docker가 왜 등장했는지 이해된다.

<table>
    <thead>
        <th>시대</th>
        <th>방식</th>
        <th>문제 또는 특징</th>
    </thead>
    <tbody>
        <tr>
            <td>물리 서버</td>
            <td>서버 한 대에 애플리케이션을 직접 설치</td>
            <td>한 앱이 자원을 독차지하면 다른 앱이 느려진다. 앱마다 서버를 따로 두면 비용이 커진다.</td>
        </tr>
        <tr>
            <td>가상 머신 (VM)</td>
            <td>하이퍼바이저 위에 VM을 여러 개 띄우고, VM마다 Guest OS를 올린다.</td>
            <td>격리가 강하고 하드웨어를 효율적으로 나눠 쓴다. 대신 OS 전체를 올리므로 무겁고 부팅이 느리다.</td>
        </tr>
        <tr>
            <td>컨테이너</td>
            <td>호스트 OS의 커널을 공유하고, 앱과 필요한 라이브러리만 묶는다.</td>
            <td>Guest OS가 없어 가볍고 빠르다. 대신 커널을 공유하므로 격리 수준은 VM보다 낮다.</td>
        </tr>
    </tbody>
</table>

**비유 :** VM은 땅 위에 단독 주택을 여러 채 짓는 것이고, 컨테이너는 건물 한 채 안을 벽으로 나눠 사무실을 여러 개를 두는 것이다. 사무실끼리는 수도와 전기를 함께 쓰니 빨리 만들 수 있지만, 단독 주택만큼 완전히 분리되지는 않는다.

<br>

### 2.1 VM vs 컨테이너

<table>
    <thead>
        <th>구분</th>
        <th>가상 머신</th>
        <th>컨테이너</th>
    </thead>
    <tbody>
        <tr>
            <td>격리 단위</td>
            <td>OS 전체</td>
            <td>프로세스</td>
        </tr>
        <tr>
            <td>커널</td>
            <td>VM마다 따로</td>
            <td>호스트 커널 공유</td>
        </tr>
        <tr>
            <td>크기</td>
            <td>수 GB</td>
            <td>수십~수백 MB</td>
        </tr>
        <tr>
            <td>시작 시간</td>
            <td>수십 초 ~ 수 분</td>
            <td>1초 안팎</td>
        </tr>
        <tr>
            <td>격리 수준</td>
            <td>강함</td>
            <td>상대적으로 약함</td>
        </tr>
        <tr>
            <td>다른 OS 실행</td>
            <td>가능 (Linux 위에 Windows)</td>
            <td>호스트와 같은 커널 계열만 가능</td>
        </tr>
        <tr>
            <td>주 용도</td>
            <td>다른 OS가 필요하거나 강한 격리가 필요할 때</td>
            <td>앱 패키징과 배포, MSA</td>
        </tr>
    </tbody>
</table>

<br>

### 2.2 컨테이너가 격리되는 원리

컨테이너는 별도의 OS가 아니라 **리눅스 커널 기능 두 가지로 격리된 평범한 프로세스**다.

<table>
    <thead>
        <th>기능</th>
        <th>하는 일</th>
        <th>결과</th>
    </thead>
    <tbody>
        <tr>
            <td>Namespace</td>
            <td>프로세스, 네트워크, 파일 시스템, 호스트명 등을 보이는 범위를 나눈다.</td>
            <td>컨테이너 안에서 ps를 치면 자기 프로세스만 보이고, 메인 프로세스가 PID 1로 보인다.</td>
        </tr>
        <tr>
            <td>cgroup</td>
            <td>CPU, 메모리 같은 자원을 얼마나 쓸 수 있는지 제한한다.</td>
            <td>한 컨테이너가 호스트 자원을 독차지 못한다.</td>
        </tr>
    </tbody>
</table>

정리하자면 Namespace는 "무엇이 보이는가", cgroup은 "얼마나 쓸 수 있는가"를 정한다. 호스트에서 보면 컨테이너 프로세스도 그냥 리눅스 프로세스 하나다. 모든 컨테이너가 커널 하나를 함께 쓰기 때문에 가볍고, 같은 이유로 VM보다 격리가 약하다. 

<br>
<hr>

## 3. Docker

<br>

### 3.1 정의

**Docker는 애플리케이션을 컨테이너로 만들고(Build), 공유하고(Ship), 실행하는(Run)** 플랫폼이다. 한 번 이미지로 만들어 두면 Docker가 설치된 곳 어디서나 같은 환경으로 실행된다. "내 PC에서는 되는데 서버에서는 안 돼요" 문제를 줄여준다.

<br>

### 3.2 핵심 용어

<table>
    <thead>
        <th>용어</th>
        <th>정의</th>
        <th>비유</th>
    </thead>
    <tbody>
        <tr>
            <td>이미지 (Image)</td>
            <td>앱과 실행에 필요한 라이브러리, 설정을 담은 읽기 전용 템플릿</td>
            <td>붕어빵 틀</td>
        </tr>
        <tr>
            <td>컨테이너 (Container)</td>
            <td>이미지를 실행한 독립된 프로세스(인스턴스). 이미지 하나로 여러 개를 만들 수 있다.</td>
            <td>틀로 찍어낸 붕어빵</td>
        </tr>
        <tr>
            <td>Dockerfile</td>
            <td>이미지를 어떻게 만들지 적은 설명서</td>
            <td>붕어빵 틀 설계도</td>
        </tr>
        <tr>
            <td>레지스트리 (Registry)</td>
            <td>이미지를 저장하고 공유하는 저장소. 대표적으로 Docker Hub</td>
            <td>틀을 보관하는 창고</td>
        </tr>
    </tbody>
</table>

클래스와 객체 관계로 생각해도 된다. 이미지가 클래스, 컨테이너가 `new`로 만든 객체다.

<br>

### 3.3 Docker 아키텍처 ⭐

Docker는 **클라이언트-서버 구조**다. 명령을 입력하는 쪽과 실제로 일하는 쪽이 분리되어 있다.

```aiignore
[Docker Client]  ──REST API──▶  [Docker Daemon (dockerd)]  ──pull/push──▶  [Registry]
  docker build                    이미지 빌드                                Docker Hub
  docker run                      컨테이너 생성·실행
  docker pull                     네트워크, 볼륨 관리
```

<table>
    <thead>
        <th>구성 요소</th>
        <th>역할</th>
    </thead>
    <tbody>
        <tr>
            <td>Docker Client</td>
            <td>사용자가 입력한 docker 명령을 REST API 요청으로 바꿔 Daemon에 보낸다. 직접 일하지 않는다.</td>
        </tr>
        <tr>
            <td>Docker Daemon</td>
            <td>Docker의 핵심 엔진. 요청을 받아 이미지 빌드, 컨테이너 생성, 네트워크와 볼륨 관리를 실제로 수행한다. 레지스트리에서 이미지를 받아오거나(pull) 올리는(push)것도 Daemon이다.</td>
        </tr>
        <tr>
            <td>Images/Containers</td>
            <td>Daemon이 관리하는 대상</td>
        </tr>
        <tr>
            <td>Registry</td>
            <td>이미지 저장소</td>
        </tr>
        <tr>
            <td>Volume / Network</td>
            <td>컨테이너가 사라져도 데이터를 남기는 저장 공간(Volume)과 컨테이너 간 통신 경로 (Network)</td>
        </tr>
    </tbody>
</table>

**헷갈렸던 지점 :** 명령어(Client)와 본체(Daemon)가 분리되어 있다는 점이다. 둘은 API로만 대화하기 때문에, 같은 PC가 아니라 **원격 서버의 Daemon에도 명령을 보낼 수 있다.** Docker 공식 문서에 따르면 둘은 보통 UNIX 소켓(`/var/run/docker.sock`)이나 네트워크 인터페이스를 통해 통신한다. 이 "명령을 받는 API 서버와 실제 일하는 본체를 나누는" 구조는 이후 배울 Kubernetes에서도 똑같이 나온다.

<br>

### 3.4 `docker run`이 하는 일

`docker run nginx`를 실행하면 내부에서 이런 순서로 일이 일어난다.

1. Client가 Daemon에 "nginx 이미지로 컨테이너를 실행해 줘"라고 요청한다.
2. Daemon이 **로컬에 이미지가 있는지** 확인한다.
3. 없으면 레지스트리(Docker Hub)에서 받아온다. (pull)
4. 이미지로 컨테이너를 만들고(create) 실행한다. (start)

같은 명령을 한번 더 실행하면 3번을 건너뛴다. 이미지가 이미 로컬에 있기 때문이다.

즉 **`docker run` = (필요하면 pull) + create + start** 다.

<br>

## 3.5 포트 연결과 자주 쓰는 옵션

컨테이너는 자기만의 네트워크 공간(Namespace)를 갖는다. 그래서 컨테이너 안의 nginx가 80 포트에서 기다리고 있어도 내 PC에서는 보이지 않는다. **`-p 호스트포트:컨테이너포트`** 로 둘을 연결해야 접속할 수 있다.

<table>
    <thead>
        <th>옵션</th>
        <th>의미</th>
    </thead>
    <tbody>
        <tr>
            <td>-d</td>
            <td>백그라운드(detached) 실행. 터미널을 붙잡지 않는다.</td>
        </tr>
        <tr>
            <td>-p 8080:80</td>
            <td>내 PC의 8080 → 컨테이너의 80 연결. 왼쪽이 호스트, 오른쪽이 컨테이너</td>
        </tr>
        <tr>
            <td>--name web</td>
            <td>컨테이너에 이름을 붙인다. 이후 ID 대신 이름으로 다룰 수 있다.</td>
        </tr>
        <tr>
            <td>-it</td>
            <td>-i(입력 유지) + -t(터미널 할당). 컨테이너 안 쉘에 들어가 대화형으로 쓸 때</td>
        </tr>
        <tr>
            <td>--rm</td>
            <td>컨테이너가 종료되면 자동 삭제</td>
        </tr>
    </tbody>
</table>

<br>

## 3.6 컨테이너 생명 주기

```aiignore
Created ──start──▶ Running ──stop──▶ Exited ──rm──▶ (삭제)
                      ▲                 │
                      └─────start───────┘
```

- `docker run`은 create와 start를 한 번에 한다.
- **컨테이너는 메인 프로세스(PID1)가 살아 있는 동안만 Running이다.** 메인 프로세스가 끝나면 컨테이너도 Exited가 된다. 그래서 `ubuntu` 이미지를 그냥 실행하면 할 일이 없어 바로 종료된다.
- Exited 상태여도 삭제(rm)하기 전까지는 컨테이너가 남아 있어서 다시 start할 수 있다.

<br>

### 3.7 Dockerfile 기초

Dockerfile은 **위에서부터 한 줄씩** 실행된다. 가장 단순한 형태는 두 줄이다.

<table>
    <thead>
        <th>명령</th>
        <th>역할</th>
    </thead>
    <tbody>
        <tr>
            <td>FROM nginx:alpine</td>
            <td>기반 이미지를 고른다.</td>
        </tr>
        <tr>
            <td>COPY index.html /usr/share/nginx/html/index.html</td>
            <td>내 파일을 이미지 안에 넣는다.</td>
        </tr>
    </tbody>
</table>

- **alpine 태그 :** Alpine Linux 기반의 경량 이미지다. 기본 이미지보다 훨씬 작아서 받고 배포하는 시간이 줄어든다.
- **`docker build -t my -nginx .`의 마지막 `.`: 빌드 컨텍스트**다. 이 디렉터리의 파일들이 Daemon으로 전달되고, `COPY`는 **이 범위 안의 파일만** 복사할 수 있다. 상위 폴더 파일을 COPY하려고 하면 실패하는 이유다. 불필요한 파일이 Daemon으로 넘어가게 하지 않으려면 `.dockerignore`로 제외한다.
- **태그 생략 :** `-t my-nginx`처럼 태그를 안 붙이면 `my-nginx:latest`로 저장된다. `latest`는 "최신"이라는 보장이 아니라 **태그를 안 썼을 때 붙는 기본 이름**일 뿐이다.

<br>
<hr>

## 4. Docker Compose 

<br>

### 4.1 왜 필요한가

실제 서비스는 컨테이너 하나로 끝나지 않는다. 앱, DB, Redis가 함께 떠야 하는데 매번 긴 `docker run` 명령을 하나씩 치는 것은 번거롭고 실수하기 쉽다.

**Docker Compose는 여러 컨테이너를 하나의 애플리케이션 단위로 YAML 파일에 정의하고, 명령 하나로 함께 띄우는 도구다.**

<table>
    <thead>
        <th>특징</th>
        <th>설명</th>
    </thead>
    <tbody>
        <tr>
            <td>설정을 파일로 관리</td>
            <td>이미지, 포트, 환경 변수, 볼륨을 compose.yaml (또는 docker-compose.yml)에 적는다. 공식 문서는 compose.yaml을 기본 이름으로 권장한다.</td>
        </tr>
        <tr>
            <td>같은 환경 재현</td>
            <td>파일만 공유하면 팀원 누구나 같은 개발 환경을 띄울 수 있다.</td>
        </tr>
        <tr>
            <td>자동 네트워크</td>
            <td>Compose로 띄운 컨테이너는 같은 네트워크에 자동으로 묶이고, 서비스 이름으로 서로를 찾는다.</td>
        </tr>
    </tbody>
</table>

<br>

## 4.2 서비스 이름으로 통신하는 원리

1. Compose는 프로젝트마다 **`프로젝트명_default`** 네트워크를 만든다.
2. 이 네트워크는 **내장 DNS**가 있다.
3. `web` 컨테이너가 `redis`라는 이름으로 접속하면, DNS가 이를 `redis` 컨테이너의 현재 IP로 바꿔준다.

컨테이너 IP는 다시 만들 때마다 바뀔 수 있지만 서비스 이름은 그대로다. 그래서 **설정 파일에는 IP 대신 서비스 이름**을 쓴다. 지난 주 실습에서 Prometheus 수집 대상을 `localhost`가 아니라 `order-api:8083`으로 적었던 것도 이 원리 때문이다.

<br>

## 4.3 Docker Compose vs Kubernetes

<table>
    <thead>
        <th>구분</th>
        <th>Docker Compose</th>
        <th>Kubernetes</th>
    </thead>
    <tbody>
        <tr>
            <td>범위</td>
            <td>서버 한 대에서 여러 컨테이너</td>
            <td>여러 서버(클러스터)에서 대규모 컨테이너</td>
        </tr>
        <tr>
            <td>설정</td>
            <td>compose.yaml 하나</td>
            <td>리소스별 YAML 매니페스트 (Deployment, Service 등)</td>
        </tr>
        <tr>
            <td>확장</td>
            <td>--scale로 수동, 자동 확장 없음</td>
            <td>부하에 따른 자동 확장(HPA)</td>
        </tr>
        <tr>
            <td>장애 복구</td>
            <td>restart 정책 정도</td>
            <td>죽은 컨테이너를 다시 띄우고 다른 노드로 재배치</td>
        </tr>
        <tr>
            <td>로드 밸런싱</td>
            <td>DNS 라운드로빈 수준</td>
            <td>Service로 로드 밸런싱과 서비스 디스커버리 내장</td>
        </tr>
        <tr>
            <td>스토리지</td>
            <td>로컬 볼륨 마운트</td>
            <td>PV, PVC</td>
        </tr>
        <tr>
            <td>배포 전략</td>
            <td>컨테이너 재생성</td>
            <td>롤링 업데이트, Canary, Blue-Green</td>
        </tr>
        <tr>
            <td>주 용도</td>
            <td>개발, 테스트 환경</td>
            <td>운영(프로덕션) 환경</td>
        </tr>
    </tbody>
</table>

한 줄로 정리하면 Compose는 "내 PC에서 여러 컨테이너를 편하게 띄우는 도구", Kubernetes는 "여러 서버에 걸쳐 컨테이너를 알아서 운영해 주는 시스템"이다.

<br>
<hr>

## 5. 핵심 정리

- 클라우드 IT 자원을 **필요한 만큼 빌리고 쓴 만큼 내는** 방식이다. 비용 구조, 속도, 탄력성, 안정성이 장점이다.
- 서비스 모델은 **사업자가 어디까지 관리하느냐 (IaaS → PaaS → SaaS)**, 배포 형태는 **누구의 인프라냐 (Private, Public, Hybrid)** 로 구분한다.
- 배포 단위는 물리 서버 → VM → 컨테이너로 바뀌었다. **VM은 OS째 격리, 컨테이너는 커널을 공유하는 프로세스 격리**다.
- 컨테이너 격리는 **Namespace(보이는 범위)** 와 **cgroup(쓸 수 있는 양)** 으로 구현된다.
- 이미지는 읽기 전용 템플릿, 컨테이너는 그 실행 인스턴스, Dockerfile은 이미지 설명서, 레지스트리는 이미지 저장소다.
- Docker는 **Client가 REST API로 Daemon에 요청하고, Daemon이 실제로 일하는** 클라이언트-서버 구조다.
- `docker run` = (필요하면 pull) + create + start. 컨테이너는 **PID 1이 살아 있는 동안만** 실행된다.
- `-p 호스트 : 컨테이너`로 포트를 연결해야 밖에서 접속할 수 있다. `COPY`는 빌드 컨텍스트 안의 파일만 가능하다.
- Compose는 여러 컨테이너를 파일 하나로 정의하고, 같은 네트워크의 **내장 DNS로 서비스 이름 통신**을 지원한다. 운영 규모의 오케스트레이션은 Kubernetes가 맡는다.


<br>