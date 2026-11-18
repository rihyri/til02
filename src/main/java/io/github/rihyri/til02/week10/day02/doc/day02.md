<br>

# Week 10 - Day 02. Dockerfile과 이미지 레지스트리

<br>

이번 수업은 **이미지가 무엇으로 이루어지는지(레이어) → 이미지를 만드는 설명서 Dockerfile → 이미지를 작고 빠르게 만드는 방법(최적화, 멀티 스테이지) → 환경별 설정과 버전 관리 → 이미지를 보관하는 레지스트리** 순서로 진행됐다.

<br>
<hr>

## 1. Docker 이미지

<br>

### 1.1 이미지와 레이어

**이미지는 앱 코드, 런타임, 라이브러리, 설정까지 실행에 필요한 것을 모두 담은 읽기 전용 패키지이다.**

이미지는 통짜 파일이 아니라 **레이어(Layer)를 차곡차곡 쌓은 구조**다. 같은 레이어는 여러 이미지가 함께 쓰기 때문에, 같은 베이스 이미지를 쓰는 이미지가 10개여도 베이스 레이어는 디스크에 한 번만 저장된다. 

```aiignore
┌─────────────────────────────┐
│ Container Layer (읽기/쓰기)  │ ← 컨테이너마다 하나. 삭제하면 같이 사라짐
├─────────────────────────────┤
│ COPY . .          (읽기 전용) │
│ RUN pip install   (읽기 전용) │ ← 이미지 레이어
│ FROM python:3.12  (읽기 전용) │
└─────────────────────────────┘
```

컨테이너를 실행하면 이미지 레이어 위에 **얇은 쓰기 가능 레이어**가 하나 올라간다. 컨테이너 안에서 파일을 고치면 이 레이어에만 기록되고, 원본 이미지는 바뀌지 않는다. 그래서 컨테이너를 지우면 그 안에서 바꾼 내용도 함께 사라진다. 데이터를 남기려면 Volume을 써야 하는 이유다.

<br>

### 1.2 이미지 이름 읽는 법

이미지 이름은 `레지스트리/계정/이미지:태그` 형식이다.

```aiignore
docker.io / library / nginx : 1.27-alpine
  레지스트리    계정       이미지     태그
```

- 레지스트리를 생략하면 **Docker Hub(`docker.io`)**, 계정을 생략하면 공식 이미지 계정인 `library`가 붙는다. 그래서 `nginx`만 써도 `docker.io/library/nginx:latest`로 해석된다.
- 태그에는 버전과 변형(variant)이 함께 들어간다. 같은 버전이라도 기본판과 경량판을 함께 제공하는 이미지는 `-alpine`, `-slim`같은 접미사로 구분한다.

<table>
    <thead>
        <th>변형</th>
        <th>특징</th>
    </thead>
    <tbody>
        <tr>
            <td>기본 (python:3.12)</td>
            <td>Debian 기반, 도구가 많아 크다.</td>
        </tr>
        <tr>
            <td>-slim</td>
            <td>Debian 기반에서 불필요한 패키지를 뺀 경량판</td>
        </tr>
        <tr>
            <td>-alpine</td>
            <td>Alpine Linux 기반, 가장 작다. 대신 glibc가 아니라 musl libc를 써서 일부 라이브러리와 호환 문제가 생길 수 있다.</td>
        </tr>
    </tbody>
</table>

Python처럼 C 확장 라이브러리를 많이 쓰는 언어는 alpine에서 빌드가 꼬이는 경우가 있어서 `slim`을 고르는 경우가 많다. 수업 예제가 `python:3.12-slim`을 쓴 이유다.

<br>

### 1.3 이미지 명령어

<table>
    <thead>
        <th>명령어</th>
        <th>하는 일</th>
    </thead>
    <tbody>
        <tr>
            <td>docker images</td>
            <td>로컬에 있는 이미지 목록</td>
        </tr>
        <tr>
            <td>docker pull 이미지</td>
            <td>레지스트리에서 이미지 내려받기</td>
        </tr>
        <tr>
            <td>docker inspect 이미지</td>
            <td>환경 변수, 실행 명령, 노출 포트 등 상세 정보</td>
        </tr>
        <tr>
            <td>docker history 이미지</td>
            <td>어떤 레이어로 만들어졌는지, 레이어별 크기</td>
        </tr>
        <tr>
            <td>docker rmi 이미지</td>
            <td>이미지 삭제</td>
        </tr>
        <tr>
            <td>docker image prune</td>
            <td>태그 없는(dangling) 이미지 정리. -a를 붙이면 컨테이너가 안 쓰는 이미지 전부</td>
        </tr>
    </tbody>
</table>

```aiignore
docker pull python:3.12-slim
docker history python:3.12-slim     # 레이어별 크기 확인
docker inspect --format '{{.Config.Cmd}}' python:3.12-slim      # 기본 실행 명령만 보기
docker image prune                  # 빌드하다 남은 <none> 이미지 정리
```

같은 이미지를 여러 번 다시 빌드하면 이전 이미지는 태그를 잃고 `<none>`으로 남는다. Java 이미지는 수백 MB씩 하기 때문에 `prune`으로 주기적으로 정리하지 않으면 디스크가 금방 찬다.

<br>
<hr>

## 2. 빌드 컨텍스트와 .dockerignore

`docker build .`의 `.`은 **빌드 컨텍스트**다. 이 디렉터리 안의 파일이 통째로 Daemon에 전송되고, Dockerfile의 `COPY`는 이 범위 안의 파일만 쓸 수 있다. 빌드 로그 첫 줄의 `transferring context: 123MB`가 전송된 크기다.

**`.dockerignore`** 는 이때 보내지 않을 파일을 적는 파일이다. `.gitignore`와 문법이 비슷하다.

```aiignore
# .dockerignore (Python 프로젝트)
__pycache__
*.pyc
.venv
venv
.env
.git
Dockerfile
.dockerignore
```

<table>
    <thead>
        <th>효과</th>
        <th>이유</th>
    </thead>
    <tbody>
        <tr>
            <td>빌드 속도 향상</td>
            <td>.git, venv 처럼 큰 디렉터리를 Daemon에 보내지 않는다.</td>
        </tr>
        <tr>
            <td>보안</td>
            <td>.env 같은 비밀 정보가 COPY . .로 이미지에 딸려 들어가는 걸 막는다.</td>
        </tr>
        <tr>
            <td>캐시 효율</td>
            <td>빌드와 무관한 파일이 바뀌어도 COPY . .레이어 캐시가 깨지지 않는다.</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

## 3. Dockerfile

<br>

### 3.1 주요 명령어

Dockerfile은 위에서 아래로 순서대로 실행된다.

<table>
    <thead>
        <th>명령어</th>
        <th>역할</th>
        <th>예시</th>
    </thead>
    <tbody>
        <tr>
            <td>FROM</td>
            <td>베이스 이미지 지정. 반드시 있어야 한다.</td>
            <td>FROM python:3.12-slim</td>
        </tr>
        <tr>
            <td>WORKDIR</td>
            <td>이후 명령의 작업 디렉터리 (없으면 만든다.)</td>
            <td>WORKDIR /app</td>
        </tr>
        <tr>
            <td>COPY</td>
            <td>빌드 컨텍스트의 파일을 이미지로 복사</td>
            <td>COPY . .</td>
        </tr>
        <tr>
            <td>RUN</td>
            <td>빌드할 때 명령 실행</td>
            <td>RUN pip install -r requirements.txt</td>
        </tr>
        <tr>
            <td>ENV</td>
            <td>컨테이너에도 남는 환경 변수</td>
            <td>ENV TZ=Asia/Seoul</td>
        </tr>
        <tr>
            <td>ARG</td>
            <td>빌드할 때만 쓰는 변수</td>
            <td>ARG PROFILE=dev</td>
        </tr>
        <tr>
            <td>EXPOSE</td>
            <td>사용하는 포트를 문서로 표시. 실제로 열지는 않는다.</td>
            <td>EXPOSE 8080</td>
        </tr>
        <tr>
            <td>USER</td>
            <td>이후 명령과 컨테이너를 실행할 사용자</td>
            <td>USER spring</td>
        </tr>
        <tr>
            <td>CMD</td>
            <td>컨테이너 시작 시 기본 명령 (바꿀 수 있음)</td>
            <td>CMD ["python", "app.py"]</td>
        </tr>
        <tr>
            <td>ENTRYPOINT</td>
            <td>컨테이너 시작 시 항상 실행할 명령</td>
            <td>ENTRYPOINT ["java", "-jar", "app.jar"]</td>
        </tr>
    </tbody>
</table>

- **`COPY . .`** : 앞의 `.`은 빌드 컨텍스트(내 PC 쪽), 뒤의 `.`은 이미지 안의 현재 `WORKDIR` 이다. "여기 있는 걸 저기로 붙여넣기"로 읽으면 된다.
- **`EXPOSE` 만으로는 접속이 안 된다.** 실제 연결은 `docker run -p`가 한다. `EXPOSE`는 이미지를 쓰는 사람에게 "5000 포트를 쓴다"고 알려주는 메모에 가깝다.

<br>

## 3.2 RUN, CMD, ENTRYPOINT ⭐

셋 다 명령을 실행하지만 **언제 실행되는지**가 다르다.

<table>
    <thead>
        <th>명령</th>
        <th>실행 시점</th>
        <th>docker run 뒤에 인자를 주면</th>
    </thead>
    <tbody>
        <tr>
            <td>RUN</td>
            <td>빌드할 때 한 번. 결과가 레이어로 저장된다.</td>
            <td>관계없음</td>
        </tr>
        <tr>
            <td>CMD</td>
            <td>컨테이너를 시작할 때마다</td>
            <td>CMD가 통째로 대체된다.</td>
        </tr>
        <tr>
            <td>ENTRYPOINT</td>
            <td>컨테이너를 시작할 때마다</td>
            <td>ENTRYPOINT 뒤에 인자로 덧붙인다.</td>
        </tr>
    </tbody>
</table>

```aiignore
# CMD ["python", "app.py"] 인 이미지
docker run my-python-app                    # python app.py 실행
docker run my-python-app python --version   # CMD가 대체되어 python --version 실행

# ENTRYPOINT ["java", "-jar", "app.jar"] 인 이미지
docker run my-spring-app --server.port=9090 # java -jar app.jar --server.port=9090
```

CMD와 ENTRYPOINT는 **"덮어쓸 수 있느냐"** 로 갈린다. 둘을 함께 쓰면 ENTRYPOINT는 고정 명령, CMD는 **기본 인자** 역할을 한다.

```aiignore
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--server.port=8080"]      # 인자를 안 주면 이 값이 붙고, 주면 이 값이 대체된다.
```

ENTRYPOINT도 꼭 바꿔야 할 때는 `docker run --entrypoint sh my-spring-app` 처럼 옵션으로 바꿀 수는 있다.

<br>
<hr>

### 3.3 exec 형식과 shell 형식

<table>
    <thead>
        <th>형식</th>
        <th>예</th>
        <th>PID 1</th>
    </thead>
    <tbody>
        <tr>
            <td>exec 형식 (JSON 배열)</td>
            <td>CMD ["python", "app.py"]</td>
            <td>python 자신</td>
        </tr>
        <tr>
            <td>shell 형식</td>
            <td>CMD python app.py</td>
            <td>/bin/sh -c가 PID 1, python은 그 자식</td>
        </tr>
    </tbody>
</table>

`docker stop`은 PID 1에 SIGTERM을 보낸다. shell 형식이면 셸이 신호를 받고 앱에 전달하지 않아, 앱이 정상종료하지 못하고 10초 뒤 강제 종료(SIGKILL)된다. **CMD와 ENDPOINT는 exec 형식으로 쓰는 것이 기본**이다.

<br>

### 3.4 Python 예제

```aiignore
FROM python:3.12-slim
WORKDIR /app

COPY requirements.txt .                             # 의존성 목록 먼저
RUN pip install --no-cache-dir -r requirements.txt  # pip 캐시를 이미지에 남기지 않음

COPY . .                                            # 자주 바뀌는 소스는 마지막
EXPOSE 5000
CMD ["python", "app.py"]
```

```aiignore
docker build -t mini-server:1.0.0 .
docker run -d -p 5000:5000 --name mini mini-server:1.0.0
curl localhost:5000/health      # hostname에 컨테이너 ID 앞 12자리가 보인다.
```

앱 코드에서 `host='0.0.0.0'`으로 바인딩한 것도 중요하다. `127.0.0.1`로 띄우면 컨테이너 **안에서만** 접속되고, `-p`로 포트를 연결해도 밖에서는 접속이 안 된다.

<br>
<hr>

## 4. 이미지 최적화

<br>

### 4.1 레이어가 만들어지는 방식

`RUN`, `COPY`, `ADD` 는 파일 시스템을 바꾸므로 **새 레이어**를 만든다. `WORKDIR`, `ENV`, `CMD` 같은 명령은 설정(메타데이터)만 바꾸므로 `docker history`에 한 줄로 보여도 크기는 거의 0이다.

<br>

### 4.2 같은 RUN 안에서 정리하기

**레이어는 위에 쌓이기만 하고, 아래 레이어의 파일을 지울 수 없다.** 위 레이어에서 `rm`을 해도 "안 보이게" 가릴 뿐, 아래 레이어에 파일이 그대로 남아 이미지 크기는 줄지 않는다.

```aiignore
# 나쁜 예 : 레이어 3개, apt 캐시가 2번째 레이어에 그대로 남는다.
RUN apt-get update
RUN apt-get install -y curl
RUN rm -rf /var/lib/apt/lists/*

# 좋은 예 : 레이어 1개, 캐시가 최종 이미지에 들어가지 않는다.
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*
```

- `--no-install-recommends` : 꼭 필요한 패키지만 설치하고 추천 패키지는 뺀다.
- `update`와 `install`을 나누면 `update` 레이어가 캐시된 채 남아서, 나중에 **오래된 패키지 목록으로 설치**하는 문제도 생긴다. 둘은 항상 한 RUN에 묶는다.


<br>

### 4.3 작은 베이스 이미지 고르기

<table>
    <thead>
        <th>베이스 이미지</th>
        <th>대략 크기</th>
        <th>특징</th>
    </thead>
    <tbody>
        <tr>
            <td>ubuntu:22.04</td>
            <td>약 80MB</td>
            <td>익숙하고 패키지가 많다.</td>
        </tr>
        <tr>
            <td>debian:bookworm-slim</td>
            <td>약 75MB</td>
            <td>최소 구성 Debian</td>
        </tr>
        <tr>
            <td>alpine:3.20</td>
            <td>약 8MB</td>
            <td>매우 작다. musl libc 호환성 주의</td>
        </tr>
        <tr>
            <td>eclipse-temurin:21-jdk</td>
            <td>약 450MB</td>
            <td>Java 컴파일러까지 포함</td>
        </tr>
        <tr>
            <td>eclipse-temurin:21-jre-alpine</td>
            <td>약 200MB</td>
            <td>Java 실행 환경만 포함</td>
        </tr>
    </tbody>
</table>

이미지가 작으면 받고 배포하는 시간이 줄고, 안에 든 프로그램이 적어 **보안 취약점이 끼어들 여지도 줄어든다.**

<br>

### 4.4 레이어 캐싱 ⭐

빌드할 때 각 명령의 **명령어와 입력(복사할 파일 내용)이 이전 빌드와 같으면** 레이어를 새로 만들지 않고 재사용한다. 핵심 규칙은 하나다.

> **한 단계의 캐시가 깨지면, 그 아래 모든 단계는 다시 실행된다.**

그래서 **잘 안바뀌는 것은 위에, 자주 바뀌는 것은 아래에** 둔다.

```aiignore
# 나쁜 순서 : 소스 한 줄만 고쳐도 pip install부터 다시 돈다.
COPY . .
RUN pip install -r requirements.txt

# 좋은 순서 : 소스만 고치면 pip install 레이어는 캐시를 쓴다.
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
```

```aiignore
docker build -t mini-server:1.0.1       # 소스만 고친 뒤 다시 빌드
# => CACHED [3/5] RUN pip install ...   ← 캐시 재사용 여부가 로그에 보인다.
```

<br>

### 4.5 멀티 스테이지 빌드

**하나의 Dockerfile에 빌드 단계와 실행 단계를 나눠 적고, 최종 이미지에는 실행에 필요한 결과만 가져오는 방식**이다.

- 1단계(builder) : JDK와 Gradle로 jar를 만든다.
- 2단계(runtime) : JRE만 있는 작은 이미지에 jar 하나만 복사한다.

JDK, Gradle, 소스 코드, 의존성 캐시가 최종 이미지에 들어가지 않아 **작고 안전하다.** 또 로컬에 JDK가 없어도 Docker만 있으면 어디서나 같은 방식으로 빌드된다.

```aiignore
# ---------- 1단계: 빌드 ----------
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon > /dev/null      # 의존성만 먼저 (캐시용)

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# ---------- 2단계: 실행 ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG PROFILE=dev
ENV SPRING_PROFILES_ACTIVE=${PROFILE}

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

<table>
    <thead>
        <th>포인트</th>
        <th>이유</th>
    </thead>
    <tbody>
        <tr>
            <td>Gradle 이미지 대신 JDK + gradlew</td>
            <td>Wrapper가 프로젝트에 맞는 Gradle 버전을 받아 쓰므로 버전 불일치로 빌드가 깨지지 않는다.</td>
        </tr>
        <tr>
            <td>설정 파일 먼저 복사 → dependencies</td>
            <td>소스만 고쳤을 때 의존성 다운로드 레이어가 캐시되어 빌드가 몇 분 → 몇십 초로 줄어든다.</td>
        </tr>
        <tr>
            <td>COPY --from=builder</td>
            <td>다른 스테이지에서 결과물만 가져온다. bootJar만 돌리므로 *.jar는 정확히 한 파일이다.</td>
        </tr>
        <tr>
            <td>USER spring</td>
            <td>root로 실행하면 앱이 뚫렸을 때 공격자가 컨테이너 root를 얻는다. 전용 사용자로 권한을 줄인다. (alpine은 addgroup -S / adduser -S, Debian 계열은 groupadd / useradd)</td>
        </tr>
        <tr>
            <td>exec 형식 ENTRYPOINT</td>
            <td>java가 PID 1이 되어 docker stop의 SIGTERM을 직접 받고 정상 종료한다.</td>
        </tr>
    </tbody>
</table>

<br>
<hr>

## 5. 환경별 설정과 이미지 관리

<br>

### 5.1 ARG와 ENV

<table>
    <thead>
        <th>구분</th>
        <th>ARG</th>
        <th>ENV</th>
    </thead>
    <tbody>
        <tr>
            <td>살아 있는 시점</td>
            <td>빌드 중에만</td>
            <td>빌드 중 + 컨테이너 실행 중</td>
        </tr>
        <tr>
            <td>값 넣는 법</td>
            <td>docker build --build-arg PROFILE=prod</td>
            <td>docker run -e KEY=값</td>
        </tr>
        <tr>
            <td>용도</td>
            <td>빌드 방식을 바꿀 때</td>
            <td>실행 중인 앱이 읽을 설정</td>
        </tr>
    </tbody>
</table>

ARG는 컨테이너가 뜰 때 사라지므로, 앱이 읽어야 하는 값이면 위 예제처럼 **`ENV 이름=${ARG 이름}` 으로 넘겨줘야** 한다.

```aiignore
docker build --build-arg PROFILE=prod -t order-api:1.2.0 .      # 기본 프로필을 prod로 굽기
docker run -e SPRING_PROFILES_ACTIVE=dev order-api:1.2.0        # 실행 시점에 다시 덮어쓰기
```

비밀번호 같은 비밀 값은 ARG로 넘기면 `docker history`에 남을 수 있다. 실행 시점에 `-e`나 시크릿 관리 도구로 주입한다.

<br>

### 5.2 dev/prod 프로필

**이미지는 하나로 두고, 실행 환경에 따라 설정만 바꾼다.**

<table>
    <thead>
        <th>파일</th>
        <th>내용</th>
    </thead>
    <tbody>
        <tr>
            <td>application.yml</td>
            <td>공통 설정. 프로필을 안 주면 dev</td>
        </tr>
        <tr>
            <td>application-dev.yml</td>
            <td>H2 인메모리 DB. DB 컨테이너 없이 단독 실행 가능, SQL 로그 출력</td>
        </tr>
        <tr>
            <td>application-prod.yml</td>
            <td>MySQL. 접속 정보는 환경 변수로 주입, SQL 로그 끔</td>
        </tr>
    </tbody>
</table>

```yaml
# application-prod.yml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://db:3306/mydb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:password}
```

`${DB_URL:기본값}`은 "환경 변수 `DB_URL`이 있으면 그 값, 없으면 콜론 뒤가 기본값"이라는 뜻이다. 기본값의 호스트가 `db`인 것은 Compose의 서비스 이름으로 접속하기 때문이다.

<br>

## 5.3 latest를 쓰면 안 되는 이유 ⭐

**`latest`는 "가장 최신"이라는 뜻이 아니라, 태그를 안 적었을 때 자동으로 붙는 이름일 뿐이다.** 그래서 같은 `latest`라도 **받은 시점에 따라 내용이 다를 수 있다.**

```aiignore
11/1    서버 A : docker pull order-api:latest     → 그 날의 latest (v1.2.0 코드)
11/10   누군가 새 이미지를 latest로 push     → latest가 v1.3.0 코드로 바뀜
11/12   서버 B : docker pull order-api:latest     → v1.3.0 코드

→ 두 서버 모두 "order-api:latest"인데 실제로는 다른 코드가 돌고 있다.
```

이러면 장애가 났을 때 **어느 버전에서 문제가 생겼는지 알 수 없고, 이전 버전으로 되돌릴 대상도 불분명하다.** 그래서 운영 배포에는 `latest` 대신 **버전 태그를 명시한다.**

```aiignore
docker build -t order-api:1.2.0 .
docker tag order-api:1.2.0 order-api:latest     # 필요하면 latest는 "별명"으로만 추가
```

<br>

## 5.4 시맨틱 버저닝

`MAJOR.MINOR.PATCH` 형식이다.

<table>
    <thead>
        <th>자리</th>
        <th>올리는 경우</th>
        <th>예</th>
        <th>의미</th>
    </thead>
    <tbody>
        <tr>
            <td>MAJOR</td>
            <td>호환성이 깨지는 변경</td>
            <td>1.5.2→2.0.0</td>
            <td>쓰는 쪽 코드도 고쳐야 할 수 있다.</td>
        </tr>
        <tr>
            <td>MINOR</td>
            <td>호환되는 기능 추가</td>
            <td>1.9.2→1.10.0</td>
            <td>기존 기능은 그대로, 새 기능 추가</td>
        </tr>
        <tr>
            <td>PATCH</td>
            <td>호환되는 버그 수정</td>
            <td>1.5.2→1.5.3</td>
            <td>기능 변화 없이 오류만 수정</td>
        </tr>
    </tbody>
</table>

- 상위 자리를 올리면 하위 자리는 0으로 초기화한다.
- `1.9` 다음은 `2.0`이 아니라 **`1.10`** 이다. 각 자리는 소수점이 아니라 독립된 정수다.

<br>
<hr>

## 6. 프라이빗 레지스트리

<br>

회사 코드가 들어 있는 이미지는 공개 저장소에 올릴 수 없다. **접근 권한을 제어할 수 있는 프라이빗 레지스트리**를 쓴다.

<table>
    <thead>
        <th>종류</th>
        <th>예</th>
    </thead>
    <tbody>
        <tr>
            <td>Docker Hub 비공개 저장소</td>
            <td>Docker Hub Private repository</td>
        </tr>
        <tr>
            <td>클라우드 레지스트리</td>
            <td>Amazon ECR, Google Artifact Registry, Azure Container Registry</td>
        </tr>
        <tr>
            <td>코드 저장소 연동</td>
            <td>GitHub Container Registry(`ghcr.io`), GitLab Container Registry</td>
        </tr>
        <tr>
            <td>직접 운영</td>
            <td>Docker 공식 registry 이미지, Harbor, Nexus</td>
        </tr>
    </tbody>
</table>

레지스트리에 올리는 흐름은 **로그인 → 레지스트리 주소가 들어간 이름으로 태그 → push**다. 이미지 이름 앞부분(레지스트리 주소)이 곧 "어디로 보낼지"를 결정한다.

```bash
docker login ghcr.io
docker tag order-api:1.2.0 ghcr.io/my-org/order-api:1.2.0
docker push ghcr.io/my-org/order-api:1.2.0

# 다른 서버에서
docker pull ghcr.io/my-org/order-api:1.2.0
```

로컬에서 연습할 때는 공식 `registry` 이미지로 레지스트리를 직접 띄워볼 수 있다.

```aiignore
docker run -d -p 5000:5000 --name registry registry:2
docker tag mini-server:1.0.0 localhost:5000/mini-server:1.0.0
docker push localhost:5000/mini-server:1.0.0
```

<br>
<hr>

## 7. 핵심 정리
- 이미지는 **읽기 전용 레이어를 쌓은 구조**이고, 컨테이너는 그 위에 쓰기 가능 레이어 하나를 얹어 실행한 것이다.
- 이미지 이름은 `레지스트리/계정/이미지:태그`. 생략하면 `docker.io/library/...:latest`가 된다.
- `.dockerignore`로 빌드 컨텍스트를 줄이면 **속도, 보안, 캐시 효율**이 모두 좋아진다.
  - `RUN`은 **빌드 때,** `CMD`와 `ENTRYPOINT`는 **실행 때.** CMD는 대체되고 ENTRYPOINT는 인자가 덧붙는다. 둘 다 **exec 형식**으로 쓴다.
  - 레이어는 지울 수 없으므로 **설치와 정리를 한 RUN에** 묶는다. 캐시는 한 번 깨지면 아래가 전부 다시 돌기 때문에 **안 바뀌는 것을 위에** 둔다.
  - 멀티스테이지 빌드는 **빌드 도구는 버리고 결과물만** 최종 이미지에 담는다. 실행은 **non-root 사용자**로 한다.
  - ARG는 빌드 중에만, ENV는 실행 중에도 남는다. 이미지 하나에 **프로필과 환경 변수**로 환경별 설정을 바꾼다.
  - `latest`는 최신이 아니라 기본 이름이다. 운영 배포는 **시맨틱 버전 태그**로 한다.
  - 회사 이미지는 **프라이빗 레지스트리**에 `tag → push`로 올린다.

<br>

