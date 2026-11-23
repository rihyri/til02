<br>

# Week 10. 클라우드와 Docker 컨테이너 운영

<br>

이번 주는 **클라우드와 컨테이너가 왜 등장했는지 → Docker가 어떻게 동작하는지(구조, Compose) → 이미지를 작고 빠르게 만드는 방법(Dockerfile, 레이어 캐시, 멀티 스테이지) → 이미지 버전 관리와 레지스트리 → 컨테이너끼리 통신하고 데이터를 남기는 방법(네트워크, 볼륨) → 컨테이너가 할 수 있는 일과 자원을 제한하는 방법(보안, JVM 메모리) → 로그와 디스크 관리** 순서로 진행됐다. 그 중 **레이어 캐시, 사용자 정의 네트워크, 볼륨, 로그 로테이션이 특히 중요하다.** 모두 놓치면 빌드가 느려지거나, 컨테이너끼리 연결이 안 되거나, 데이터가 사라지거나, 디스크에 가득 차는 문제로 이어진다.

<br>
<hr>

## 1. 클라우드와 컨테이너

<br>

### 1.1 클라우드 컴퓨팅

클라우드는 서버, 스토리지, 네트워크 같은 IT 자원을 **필요할 때 인터넷으로 빌려 쓰고, 쓴 만큼 비용을 내는 방식**이다. 서버를 미리 사 두는 초기 투자가 운영비로 바뀌고, 트래픽에 따라 자원을 늘리거나 줄일 수 있다.

<table>
    <thead>
        <tr>
			<th>서비스 모델</th>
			<th>제공받는 범위</th>
			<th>사용자가 관리하는 범위</th>
			<th>예시</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>IaaS</td>
            <td>가상 서버, 네트워크, 스토리지</td>
            <td>OS부터 애플리케이션, 데이터까지</td>
            <td>AWS EC2</td>
        </tr>
        <tr>
            <td>PaaS</td>
            <td>애플리케이션 실행 플랫폼</td>
            <td>애플리케이션 코드와 데이터</td>
            <td>Elastic Beanstalk</td>
        </tr>
        <tr>
            <td>SaaS</td>
            <td>완성된 소프트웨어</td>
            <td>설정과 사용</td>
            <td>Gmail, Slack</td>
        </tr>
    </tbody>
</table>

- 서비스 모델은 **사용자가 어디까지 관리해 주는가**로 나눈다.
- 배포 형태(Private, Public, Hybrid)는 **누구의 인프라를 쓰는가**로 나눈다.

<br>

### 1.2 VM과 컨테이너

<table>
    <thead>
        <tr>
			<th>구분</th>
			<th>가상 머신 (VM)</th>
			<th>컨테이너</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>격리 단위</td>
            <td>Guest OS를 포함한 OS 전체</td>
            <td>프로세스</td>
        </tr>
        <tr>
            <td>커널</td>
            <td>VM마다 따로</td>
            <td>호스트 커널 공유</td>
        </tr>
        <tr>
            <td>크기/시작 속도</td>
            <td>수 GB / 수십 초 이상</td>
            <td>수십~수백 MB/1초 안팎</td>
        </tr>
        <tr>
            <td>격리 수준</td>
            <td>강함</td>
            <td>상대적으로 약함</td>
        </tr>
    </tbody>
</table>

<br>

### 1.3 컨테이너가 격리되는 원리

컨테이너는 별도의 OS가 아니라 **리눅스 커널 기능으로 격리된 평범한 프로세스**다.

<table>
    <thead>
        <tr>
			<th>기능</th>
			<th>정하는 것</th>
			<th>결과</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>Namespace</td>
            <td>무엇이 보이는가 (프로세스, 네트워크, 파일 시스템)</td>
            <td>컨테이너 안에서는 메인 프로세스가 PID 1으로 보인다.</td>
        </tr>
        <tr>
            <td>cgroup</td>
            <td>얼마나 쓸 수 있는가 (CPU, 메모리)</td>
            <td>컨테이너 하나가 호스트 자원을 독차지하지 못한다.</td>
        </tr>
    </tbody>
</table>

커널을 함께 쓰기 때문에 가볍고 빠르다. 같은 이유로 VM보다 격리가 약하다.

<br>

## 2. Docker 구조와 Compose 

<br>

### 2.1 핵심 용어

<table>
    <thead>
        <tr>
			<th>용어</th>
			<th>설명</th>
			<th>비유</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>Image</td>
            <td>앱과 실행 환경을 담은 읽기 전용 템플릿</td>
            <td>클래스</td>
        </tr>
        <tr>
            <td>Container</td>
            <td>이미지를 실행한 인스턴스</td>
            <td>new로 만든 객체</td>
        </tr>
        <tr>
            <td>Dockerfile</td>
            <td>이미지를 만드는 순서를 적은 파일</td>
            <td>설계도</td>
        </tr>
        <tr>
            <td>Registry</td>
            <td>이미지를 저장하고 공유하는 저장소 (Docker Hub, ECR, GHCR)</td>
            <td>창고</td>
        </tr>
    </tbody>
</table>


<br>

### 2.2 클라이언트-서버 구조 ⭐

```aiignore
[Docker Client] ──REST API──▶ [Docker Daemon (dockerd)] ──pull / push──▶ [Registry]
 docker build / run             이미지 빌드, 컨테이너 실행,
                                네트워크와 볼륨 관리
```

- `docker` 명령어는 **요청만 보내고, 실제 작업은 Daemon이 한다.** 둘은 API로만 통신하기 때문에 원격 서버의 Daemon에도 명령을 보낼 수 있다.
- `docker run` = **(로컬에 이미지가 없으면  pull) + create + start**
- 그래서 직접 빌드해야 하는 이미지를 빌드하기 전에 실행하면, Daemon이 레지스트리에서 찾으려다 `pull access denied` 오류가 난다.

<br>

### 2.3 컨테이너 생명 주기

```aiignore
Created ──start──▶ Running ──stop──▶ Exited ──rm──▶ (삭제)
                      ▲                 │
                      └──────start──────┘
```

- 컨테이너는 **메인 프로세스(PID 1)가 살아 있는 동안만 Running**이다. 할 일이 없는 이미지를 그냥 실행하면 바로 Exited가 된다.
- Exited 상태여도 삭제하기 전까지는 다시 시작하거나 로그를 볼 수 있다.

<br>

### 2.4 포트 연결과 자주 쓰는 옵션

컨테이너는 자기만의 네트워크 공간을 가지므로, 밖에서 접속하려면 **`-p 호스트포트: 컨테이너포트`** 로 연결해야 한다.

<table>
    <thead>
        <tr>
			<th>옵션</th>
			<th>의미</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>-d</td>
            <td>백그라운드 실행</td>
        </tr>
        <tr>
            <td>-p 8080:80</td>
            <td>내 PC 8080 → 컨테이너 80 (왼쪽이 호스트)</td>
        </tr>
        <tr>
            <td>--name web</td>
            <td>컨테이너 이름 지정</td>
        </tr>
        <tr>
            <td>-it</td>
            <td>컨테이너 안 쉘 대화형으로 접속</td>
        </tr>
        <tr>
            <td>--rm</td>
            <td>종료되면 자동 삭제</td>
        </tr>
    </tbody>
</table>

```bash
docker run -d --name web -p 8080:80 nginx:alpine
docker ps -a    # 종료된 컨테이너까지 확인
docker rm -f web
```

<br>

### 2.5 Docker Compose 

Compose는 여러 컨테이너를 **YAML 파일 하나에 정의하고 명령 하나로 함께 실행**하는 도구다.

- Compose는 프로젝트마다 `<프로젝트명>_default` 네트워크를 만들고 모든 서비스를 연결한다.
- 이 네트워크는 내장 DNS 덕분에 **IP 대신 서비스 이름으로** 서로 접속한다.

```yaml
services:
  redis:
    image: redis:7.4-alpine
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      retries: 5
  app:
    build: ./redis-app
    environment:
      SPRING_DATA_REDIS_HOST: redis        # 서비스 이름으로 접속
    depends_on:
      redis:
        condition: service_healthy         # "시작됨"이 아니라 "준비 완료"를 기다린다
```

> `depends_on`만 쓰면 Redis 컨테이너가 **시작된 것**만 기다린다. 요청을 받을 **준비가 됐는지**까지 기다리려면 `healthcheck`와 `condition: service_healthy`를 함께 쓴다.

<table>
    <thead>
        <tr>
			<th>구분</th>
			<th>Docker Compose</th>
			<th>Kubernetes</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>범위</td>
            <td>서버 한 대</td>
            <td>여러 서버로 이루어진 클러스터</td>
        </tr>
        <tr>
            <td>확장 / 복구</td>
            <td>수동 확장, restart 정책 정도</td>
            <td>자동 확장, 다른 노드로 재배치</td>
        </tr>
        <tr>
            <td>주 용도</td>
            <td>개발, 테스트 환경</td>
            <td>운영 환경</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

## 3. Dockerfile과 이미지 최적화

<br>

### 3.1 이미지 레이어

이미지는 **읽기 전용 레이어를 차례로 쌓은 구조**이고, 컨테이너는 그 위에 얇은 쓰기 레이어를 하나 얹어 실행한 것이다.

```aiignore
┌───────────────────────────────┐
│ 컨테이너 쓰기 레이어           │ ← 컨테이너를 지우면 함께 사라진다
├───────────────────────────────┤
│ COPY src           (읽기 전용) │
│ RUN gradlew ...    (읽기 전용) │ ← 이미지 레이어
│ FROM temurin:21    (읽기 전용) │
└───────────────────────────────┘
```

- 같은 레이어는 여러 이미지가 공유하므로 디스크 한 번만 저장된다.
- `RUN`, `COPY`, `ADD`는 새 레이어를 만들고, `ENV`, `CMD` 같은 설정 명령은 크기가 거의 없다.
- 이미지 이름은 `레지스트리/계정/이미지:태그` 형식이다. 생략하면 `docker.io/library/...:latest`가 된다.

<br>

### 3.2 RUN, CMD, ENTRYPOINT ⭐

<table>
    <thead>
        <tr>
			<th>명령</th>
			<th>실행 시점</th>
			<th>docker run 뒤에 인자를 주면</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>RUN</td>
            <td>이미지를 빌드할 때</td>
            <td>관계없음</td>
        </tr>
        <tr>
            <td>CMD</td>
            <td>컨테이너를 시작할 때</td>
            <td>통째로 대체된다.</td>
        </tr>
        <tr>
            <td>ENTRYPOINT</td>
            <td>컨테이너를 시작할 때</td>
            <td>뒤에 인자로 덧붙는다.</td>
        </tr>
    </tbody>
</table>

- 둘을 함께 쓰면 `ENTRYPOINT`는 고정 명령, `CMD`는 기본 인자 역할을 한다.
- `CMD`와 `ENTRYPOINT`는 **exec 형식 (JSON 배열)** 으로 쓴다. shell 형식이면 `/bin/sh`가 PID 1이 되어 `docker stop`의 SIGTERM이 앱까지 전달되지 않고, 유예 시간 뒤 강제 종료(SIGKILL) 된다.

<br>

### 3.3 같은 RUN 안에서 정리하기

**레이어는 위에 쌓이기만 하고 아래 레이어의 파일을 지울 수 없다.** 뒤의 `RUN` 에서 `rm`을 해도 가려질 뿐 이미지 크기는 줄지 않는다.

```dockerfile
# 나쁜 예 : apt 캐시가 아래 레이어에 그대로 남는다.
RUN apt-get update
RUN apt-get install -y curl
RUN rm -rf /var/lib/apt/lists/*

# 좋은 예 : 설치와 정리를 한 레이어에서
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*
```

<br>

### 3.4 레이어 캐시 ⭐⭐

빌드할 때 명령과 입력 파일이 이전과 같으면 레이어를 재사용한다. 규칙은 하나다.

> **한 단계에서 캐시가 깨지면 그 아래 단계는 모두 다시 실행된다.**

그래서 **잘 바뀌지 않는 것(빌드 설정, 의존성)은 위에, 자주 바뀌는 것(소스 코드)은 아래에** 둔다.

```aiignore
# 나쁜 순서 : 소스 한 줄만 고쳐도 의존성 다운로드부터 다시 실행
COPY . .
RUN ./gradlew bootJar

# 좋은 순서 : 소스만 고치면 의존성 레이어는 캐시 사용
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon > /dev/null
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon
```

<br>

### 3.5 빌드 컨텍스트와 .dockerignore

`docker build .`의 `.`은 **빌드 컨텍스트**다. 이 폴더의 파일이 Daemon으로 전송되고, `COPY`는 이 범위 안의 파일만 쓸 수 있다.

<table>
    <thead>
        <tr>
			<th>.dockerignore 효과</th>
			<th>이유</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>빌드 속도 향상</td>
            <td>.git, build 같은 큰 폴더를 보내지 않는다.</td>
        </tr>
        <tr>
            <td>보안</td>
            <td>.env 같은 비밀 정보가 COPY . .로 이미지에 들어가는 것을 막는다.</td>
        </tr>
        <tr>
            <td>캐시 효율</td>
            <td>빌드와 무관한 파일이 바뀌어도 캐시가 깨지지 않는다.</td>
        </tr>
    </tbody>
</table>

<br>

### 3.6 멀티 스테이지 빌드 ⭐

하나의 Dockerfile에서 **빌드 단계와 실행 단계를 나누고, 최종 이미지에는 실행에 필요한 결과물만** 가져온다.

```dockerfile
# 1단계: JDK로 jar 빌드
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon > /dev/null
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# 2단계: JRE만 있는 이미지에 jar만 복사
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 spring
USER spring
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

<table>
    <thead>
        <tr>
			<th>포인트</th>
			<th>이유</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>JDK, Gradle, 소스를 최종 이미지에서 제외</td>
            <td>이미지가 작아지고, 안에 든 프로그램이 보안 취약점도 줄어든다.</td>
        </tr>
        <tr>
            <td>COPY --from=builder</td>
            <td>다른 스테이지의 결과물만 가져온다.</td>
        </tr>
        <tr>
            <td>USER로 일반 사용자 실행</td>
            <td>앱이 공격당해도 컨테이너 root 권한을 얻지 못한다.</td>
        </tr>
        <tr>
            <td>exec형식 ENTRYPOINT</td>
            <td>java가 PID 1이 되어 SIGTERM을 받아 정상 종료한다.</td>
        </tr>
    </tbody>
</table>

> Windows에서 받은 `gradlew`는 줄바꿈이 CRLF라서 리눅스 컨테이너 안에서 실행이 실패할 수 있다. 이럴 때는 빌드 단계에서 `sed -i 's/\r$//' gradlew`로 줄바꿈을 정리한다.

<br>

### 3.7 ARG와 ENV

<table>
    <thead>
        <tr>
			<th>구분</th>
			<th>ARG</th>
			<th>ENV</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>유효 시점</td>
            <td>빌드 중에만</td>
            <td>빌드 중 + 컨테이너 실행 중</td>
        </tr>
        <tr>
            <td>값 지정</td>
            <td>docker build --build-arg</td>
            <td>docker run -e</td>
        </tr>
        <tr>
            <td>용도</td>
            <td>빌드 방식을 바꿀 때</td>
            <td>실행 중인 앱이 읽을 설정</td>
        </tr>
    </tbody>
</table>

- **이미지는 하나만 만들고, 환경별 차이는 프로필과 환경 변수로 실행 시점에 바꾼다.**
- 비밀번호를 `ARG`로 넘기면 `docker history`에 남을 수 있다. 비밀 값은 실행 시점에 주입한다.

<br>

## 4. 이미지 버전과 레지스트리

<br>

### 4.1 latest를 쓰면 안 되는 이유 ⭐

`latest`는 "가장 최신"이라는 보장이 아니라 **태그를 생략했을 때 붙는 기본 이름**일 뿐이다.

```aiignore
11/1   서버 A : docker pull order-api:latest  → v1.2.0 코드
11/10  누군가 새 이미지를 latest 로 push
11/12  서버 B : docker pull order-api:latest  → v1.3.0 코드

→ 두 서버 모두 "latest"인데 실제로는 다른 코드가 실행 중
```

장애가 나도 어떤 버전에 생긴 문제인지, 어디로 롤백해야 하는지 알 수 없다. 그래서 운영 배포에는 **버전 태그를 명시한다.**

<br>

### 4.2 시맨틱 버저닝

<table>
    <thead>
        <tr>
			<th>자리</th>
			<th>올리는 경우</th>
			<th>예</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>MAJOR</td>
            <td>호환이 깨지는 경우</td>
            <td>1.5.2 → 2.0.0</td>
        </tr>
        <tr>
            <td>MINOR</td>
            <td>호환되는 기능 추가</td>
            <td>1.9.2 → 1.10.0</td>
        </tr>
        <tr>
            <td>PATCH</td>
            <td>호환되는 버전 수정</td>
            <td>1.5.2 → 1.5.3</td>
        </tr>
    </tbody>
</table>

- 상위 자리를 올리면 하위 자리는 0으로 초기화한다.
- `1.9` 다음은 `2.0`이 아니라 `1.10`이다. 각 자리는 독립된 정수이다.

<br>

### 4.3 프라이빗 레지스트리

회사 코드가 담긴 이미지는 접근 권한을 제어할 수 있는 프라이빗 레지스트리(ECR, GHCR, Harbor 등)에 올린다. 흐름은 **로그인 → 레지스트리 주소를 붙여 태그 → push**이다.

```bash
docker login ghcr.io
docker tag order-api:1.2.0 ghcr.io/my-org/order-api:1.2.0
docker push ghcr.io/my-org/order-api:1.2.0
```

<br>
<hr>

## 5. 네트워크와 볼륨

<br>

### 5.1 컨테이너 통신 방향

<table>
    <thead>
        <tr>
			<th>통신 방향</th>
			<th>방법</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>호스트 → 컨테이너</td>
            <td>-p 호스트포트:컨테이너포트로 연결</td>
        </tr>
        <tr>
            <td>컨테이너 → 컨테이너</td>
            <td>같은 네트워크에 있으면 포트 연결 없이 이름:컨테이너포트로 접속</td>
        </tr>
    </tbody>
</table>

> **컨테이너 안의 `localhost`는 호스트가 아니라 그 컨테이너 자신이다.** 다른 컨테이너에 접속할 때는 서비스 이름을 쓴다.

<br>

### 5.2 기본 bridge와 사용자 정의 bridge ⭐⭐

<table>
    <thead>
        <tr>
			<th>구분</th>
			<th>기본 bridge</th>
			<th>사용자 정의 bridge</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>만드는 법</td>
            <td>Docker 설치 시 자동 생성</td>
            <td>docker network create 이름</td>
        </tr>
        <tr>
            <td>이름으로 통신</td>
            <td>불가</td>
            <td>가능 (내장 DNS)</td>
        </tr>
        <tr>
            <td>IP로 통신</td>
            <td>가능</td>
            <td>가능</td>
        </tr>
    </tbody>
</table>

IP는 컨테이너를 다시 만들면 바뀔 수 있으므로, **컨테이너끼리 이름으로 통신하려면 사용자 정의 네트워크를 쓴다.** Compose는 이 네트워크를 자동으로 만들어 준다.

```aiignore
docker network create mynetwork
docker run -d --name redis --network mynetwork redis:7.4-alpine
docker run -d --name app --network mynetwork -e SPRING_DATA_REDIS_HOST=redis my-app:1.0.0
```

- 네트워크가 다르면 IP를 알아도 통신할 수 없다.
- Compose에서 `networks`를 나누면 DB를 앞단 웹 서버와 격리할 수 있다.

<br>

### 5.3 그 밖의 네트워크 드라이버

<table>
    <thead>
        <tr>
			<th>드라이버</th>
			<th>특징</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>host</td>
            <td>호스트 네트워크를 그대로 사용. 포트 연결이 필요 없고 빠르지만, 격리가 없고 포트 충돌이 생긴다.</td>
        </tr>
        <tr>
            <td>none</td>
            <td>네트워크 없음. 루프백(lo)만 존재</td>
        </tr>
        <tr>
            <td>overlay</td>
            <td>여러 호스트를 하나의 네트워크처럼 연결 (Kubernetes에서 다룸)</td>
        </tr>
    </tbody>
</table>

<br>

### 5.4 데이터 보존과 스토리지 유형

컨테이너 쓰기 레이어에 저장한 데이터는 **컨테이너를 삭제하면 함께 사라진다.** DB 컨테이너를 다시 만들면 데이터가 모두 없어진다.

<table>
    <thead>
        <tr>
			<th>유형</th>
			<th>저장 위치</th>
			<th>용도</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>Volume</td>
            <td>Docker가 관리하는 호스트 영역</td>
            <td>DB 데이터 같은 운영 데이터</td>
        </tr>
        <tr>
            <td>Bind Mount</td>
            <td>내가 지정한 호스트 경로</td>
            <td>개발 중 소스 공유, 설정 파일 주입</td>
        </tr>
        <tr>
            <td>tmpfs</td>
            <td>메모리</td>
            <td>디스크에 남기면 안 되는 임시 데이터</td>
        </tr>
    </tbody>
</table>

```bash
docker run -d -v redis-data:/data redis:7.4-alpine                    # 이름 → Volume
docker run -d -v ./nginx.conf:/etc/nginx/nginx.conf:ro nginx:alpine   # 경로 → Bind Mount (:ro 읽기 전용)
```

<br>

### 5.5 Volume vs Bind Mount ⭐


<table>
    <thead>
        <tr>
			<th>구분</th>
			<th>Volume</th>
			<th>Bind Mount</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>관리</td>
            <td>docker volume 명령</td>
            <td>일반 파일, 폴더처럼</td>
        </tr>
        <tr>
            <td>독립성</td>
            <td>호스트 경로 구조와 무관 (운영에 적합)</td>
            <td>호스트 경로에 의존 (로컬 개발에 적합)</td>
        </tr>
        <tr>
            <td>초기 데이터</td>
            <td>빈 볼륨이면 이미지의 기존 파일을 복사해 온다.</td>
            <td>호스트 폴더가 컨테이너 경로를 덮어쓴다.</td>
        </tr>
    </tbody>
</table>


- 판단 기준은 **"이 파일을 호스트에서 직접 열어볼 일이 있는가?"** 다. 있으면 Bind Mount, 없으면 Volume
- 볼륨이 **앱이 실제로 데이터를 쓰는 경로**에 연결해야 한다. 경로가 틀리면 데이터가 다른 곳에 쌓여 재시작 후 사라진 것처럼 보인다. (Redis는 `/data`)

<br>
<hr>

## 6. 보안과 자원 제한

<br>

### 6.1 컨테이너가 할 수 있는 일 줄이기 

컨테이너는 호스트 커널을 공유하므로 **할 수 있는 일을 최소한으로 줄이는 것**이 보안의 기본이다.

<table>
    <thead>
        <tr>
			<th>옵션</th>
			<th>막는 것</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>Dockerfile USER / --user</td>
            <td>root로 실행되는 것</td>
        </tr>
        <tr>
            <td>--cap-drop ALL</td>
            <td>필요 없는 root 권한 (Capability)</td>
        </tr>
        <tr>
            <td>--read-only</td>
            <td>파일 시스템 변조</td>
        </tr>
        <tr>
            <td>--security-opt no-new-privileges</td>
            <td>setuid, sudo로 권한을 올리는 것</td>
        </tr>
        <tr>
            <td>--memory, --cpus</td>
            <td>컨테이너 하나가 서버 자원을 독차지하는 것</td>
        </tr>
    </tbody>
</table>

`--user`는 실행할 때마다 적어야 해서 빠뜨리기 쉽다. **Dockerfile에 `USER`를 지정하는 것**이 가장 좋다.

<br>

### 6.2 컨테이너 메모리 제한과 JVM ⭐

Java 앱은 **힙 외에도 메모리를 쓴다.** Metaspace, 스레드 스택, Code Cache, Direct Buffer가 힙밖에서 추가로 사용된다.

<table>
    <thead>
        <tr>
			<th>상황</th>
			<th>결과</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>힙 + 힙 외 메모리가 컨테이너 제한을 넘음</td>
            <td>커널이 강제 종료. OOMKilled=true, 종료 코드 137 (128 + SIGKILL 9). JVM 로그와 힙 덤프가 남지 않는다.</td>
        </tr>
        <tr>
            <td>힙이 먼저 가득 참</td>
            <td>JVM이 OutOfMemoryError를 던진다. 컨테이너는 살아 있고 로그와 힙 덤프로 원인을 추적할 수 있다.</td>
        </tr>
    </tbody>
</table>

그래서 `-Xmx`로 힙을 고정하기보다 **`-XX:MaxRamPercentage`로 컨테이너 메모리 대비 힙 비율을 정하고,** 힙 외 영역이 쓸 여유를 남긴다.

```bash
docker run -d --memory 512m --memory-swap 512m \
  -e JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+HeapDumpOnOutOfMemoryError" \
  my-app:1.0.0

docker inspect <컨테이너> --format '{{.State.OOMKilled}} {{.State.ExitCode}}'
```

<br>
<hr>

## 7. 로그와 디스크 관리

<br>

### 7.1 컨테이너 로그가 쌓이는 곳

```aiignore
PID 1 의 stdout / stderr
        │
        ▼
Docker Daemon → 로그 드라이버 (기본 json-file)
        │
        ▼
호스트의 <ID>-json.log  ◀── docker logs 가 읽는 파일
```

- `docker logs`는 컨테이너 안이 아니라 **호스트에 저장된 로그 파일을 읽는다.** 그래서 종료된 컨테이너도 삭제 전까지 로그를 볼 수 있다.
- 컨테이너 앱은 로그를 **파일이 아니라 stdout/stderr**로 출력한다. 파일로 쓰면 `docker logs`에 보이지 않고 컨테이너와 함께 사라진다.
- `docker exec`로 실행한 명령의 출력은 PID 1의 출력이 아니라서 로그에 남지 않는다.

<br>

### 7.2 로그 골라 보기 ⭐

**파이프(`|`)는 stdout만 넘긴다.** stderr로 나온 에러까지 검색하려면 `2>&1`로 합친다.

```bash
docker logs app | grep ERROR                          # stderr 에러는 못 찾는다
docker logs app 2>&1 | grep ERROR                     # stderr 를 합친 뒤 검색
docker logs --tail 100 -f app                         # 최근 100줄부터 실시간으로
docker logs --since 30m app 2>&1 | grep -i error      # 최근 30분 에러 검색
```

- `-f`는 `--tail`과 함께 쓴다. `Ctrl+C`는 로그 보기만 멈추고 컨테이너는 멈추지 않는다.
- `-t`로 붙는 시각은 **UTC**다. `--since`, `--until`에 시간대 없이 준 날짜는 **내 PC 시간대**로 해석된다.

<br>

### 7.3 로그 로테이션 ⭐⭐

**json-file 로그는 크기 제한이 없다.** 오래 실행하면 로그 파일이 디스크를 가득 채워 서비스 전체가 멈출 수 있다.

```bash
docker run -d --log-opt max-size=10m --log-opt max-file=3 my-app:1.0.0
# 최대 10MB × 3개 = 약 30MB
```

운영 서버에서는 **`daemon.json`에 기본값을 둔다.**

```json
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}
```

> `daemon.json`의 기본 값은 **설정 이후 새로 만든 컨테이너에만** 적용된다. Compose에서는 서비스 별 `logging:`으로 지정할 수 있다.

<br>

### 7.4 자원 사용량 모니터링

`docker stats`는 컨테이너별 CPU, 메모리, 네트워크, 프로세스 수를 실시간으로 보여준다.

<table>
    <thead>
        <tr>
			<th>증상</th>
			<th>의심할 것</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>메모리가 시간이 지날수록 계속 늘어난다.</td>
            <td>메모리 누수</td>
        </tr>
        <tr>
            <td>CPU 사용률이 계속 높다.</td>
            <td>무한 루프, 과부하</td>
        </tr>
        <tr>
            <td>PIDS가 계속 늘어난다.</td>
            <td>스레드 누수</td>
        </tr>
    </tbody>
</table>

<br>

### 7.5 디스크 정리 ⭐

Docker를 오래 쓰면 이전 버전 이미지, 중지된 컨테이너, 빌드 캐시, 볼륨이 계속 쌓인다.

```bash
docker system df                               # 1) 무엇이 얼마나 쌓였는지 확인
docker image prune --filter "until=168h"       # 2) 일주일 지난 태그 없는 이미지 정리
docker builder prune --filter "until=168h"
docker system df                               # 3) 정리 후 다시 확인
```

<table>
    <thead>
        <tr>
			<th>명령</th>
			<th>주의할 점</th>
		</tr>
    </thead>
    <tbody>
        <tr>
            <td>docker volume prune -a</td>
            <td>연결되지 않은 이름 있는 볼륨까지 지운다. DB 데이터가 복구 불가능하게 사라질 수 있다.</td>
        </tr>
        <tr>
            <td>docker image prune -a</td>
            <td>컨테이너가 쓰지 않는 모든 이미지를 지운다. 롤백용 이전 버전 이미지도 사라질 수 있다.</td>
        </tr>
        <tr>
            <td>docker system prune</td>
            <td>볼륨은 기본적으로 제외된다.</td>
        </tr>
    </tbody>
</table>

운영 서버에서는 **`-f` 없이** 실행해 확인 메시지를 읽고 진행한다.

<br>

## 8. 핵심 정리

- 클라우드는 자원을 필요한 만큼 빌려 쓰는 방식이고, 컨테이너는 **커널을 공유하는 프로세스 격리(Namespace, cgroup)** 로 가볍게 실행된다.
- Docker는 **Client가 요청하고 Daemon이 일하는** 구조다. `docker run` = (필요하면 pull) + create + start이고, 컨테이너는 **PID 1이 살아 있는 동안만** 실행된다.
- Compose는 서비스 이름으로 컨테이너를 연결하고, 준비 순서는 `healthcheck` + `condition: service_healthy`로 보장한다.
- 레이어는 지울 수 없으니 **설치와 정리를 한 RUN에,** 캐시는 **안 바뀌는 것을 위에,** 최종 이미지는 **멀티 스테이지로 결과물만** 담는다. CMD/ENTRYPOINT는 exec 형식으로 쓴다.
- `latest` 대신 **시맨틱 버전 태그**를 쓰고, 회사 이미지는 프라이빗 레지스트리에 tag → push 한다.
- **기본 bridge는 이름 DNS가 없다.** 이름으로 통신하려면 사용자 정의 네트워크를 쓰고, 컨테이너 안의 `localhost`는 자기 자신이다.
- 운영 데이터는 **Volume**에, 앱이 실제로 쓰는 경로에 연결한다. 빈 Volume은 기존 파일을 복사해오고, Bind Mount는 덮어쓴다.
- 보안은 **non-root, Capability 제거, 읽기 전용, 권한 상승 방지**로 줄이고, JVM은 힙 외 메모리를 고려해 `MaxRamPercentage`로 힙을 정한다. 137은 커널 강제 종료, OOME는 JVM이 감지한 힙 부족이다.
- 로그는 stdout으로 내보내고 `2>&1`로 검색하며, json-file은 **로테이션으로 크기를 제한**한다. prune은 지우는 대상을 확인한 뒤 실행한다.

<br>

