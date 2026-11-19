<br>

# Week 10 - Day 03. Docker 네트워크 · 볼륨과 보안

<br>

이번 수업은 **컨테이너끼리, 그리고 컨테이너와 바깥이 어떻게 통신하는지(네트워크) → 컨테이너를 지워도 데이터를 남기는 방법(볼륨) → 컨테이너가 할 수 있는 일을 줄이는 방법(보안, 자원 제한)** 순서로 진행됐다. 그 중 **네트워크와 볼륨이 특히 중요하다.**

<br>
<hr>

## 1. Docker 네트워크

<br>

### 1.1 개념

**Docker 네트워크는 컨테이너 간, 그리고 컨테이너와 외부(호스트, 인터넷) 간의 통신을 관리하는 가상 네트워크다.** 실제 케이블이나 공유기가 있는 것이 아니라 Docker가 소프트웨어로 만든다.

컨테이너는 격리된 환경에서 실행되며, 각자 **자신만의 네트워크 인터페이스와 IP 주소**를 가진다. MSA처럼 서비스 여러 개를 컨테이너로 띄우면 이들이 서로를 찾고 통신하는 방식이 이 네트워크 설정에 달려 있다.

```aiignore
[내 PC (호스트)]                [Docker 네트워크: my-network]

 브라우저                         web 컨테이너                    db 컨테이너
 localhost:8080  ──(-p 8080:80)──▶ nginx :80  ──(db:3306)──▶  mysql :3306
                                                (포트 매핑 불필요)

 호스트 → 컨테이너 : -p 필요
 컨테이너 → 컨테이너 : 같은 네트워크면 이름:컨테이너포트로 바로 통신
```

<table>
    <thead>
        <th>통신 방향</th>
        <th>방법</th>
    </thead>
    <tbody>
        <tr>
            <td>호스트 → 컨테이너</td>
            <td>-p 호스트포트:컨테이너포트로 호스트에 들어온 요청을 컨테이너 포트로 전달한다.</td>
        </tr>
        <tr>
            <td>컨테이너 → 컨테이너</td>
            <td>포트 매핑이 필요없다. 같은 네트워크 안에서 컨테이너이름:컨테이너포트로 접속한다.</td>
        </tr>
    </tbody>
</table>

컨테이너끼리 통신하려면 **같은 네트워크(같은 공유기)에 묶여 있어야 한다.**

`-p` 앞에 호스트 IP를 붙이면 그 IP로 들어온 요청만 받는다. `127.0.0.1`을 지정하면 **내 PC에서만** 접속할 수 있고, 같은 와이파이의 다른 PC에서는 접속할 수 없다.

```bash
docker run -d -p 8080:80 nginx            # 모든 인터페이스(0.0.0.0)에서 접속 가능
docker run -d -p 127.0.0.1:8080:80 nginx  # 내 PC에서만 접속 가능
```

<br>

### 1.2 네트워크 드라이버

<table>
    <thead>
        <th>드라이버</th>
        <th>설명</th>
        <th>주요 특징</th>
        <th>사용 예시</th>
    </thead>
    <tbody>
        <tr>
            <td>bridge</td>
            <td>기존 네트워크 드라이버</td>
            <td>단일 호스트 안의 가상 네트워크. 사용자 정의 bridge는 컨테이너 이름 DNS 지원</td>
            <td>단일 호스트 내 컨테이너 간 통신 (Compose 기본값)</td>
        </tr>
        <tr>
            <td>host</td>
            <td>호스트 네트워크를 그대로 사용</td>
            <td>포트 매핑 불필요, NAT가 없어 성능 유리, 격리 약함</td>
            <td>성능이 중요한 애플리케이션</td>
        </tr>
        <tr>
            <td>none</td>
            <td>네트워크 없음</td>
            <td>외부 연결 불가, 루프백(lo)만 존재</td>
            <td>네트워크가 필요 없는 배치 작업</td>
        </tr>
        <tr>
            <td>overlay</td>
            <td>여러 호스트를 하나의 네트워크로 지원</td>
            <td>Docker Swarm 필요, 서비스 이름 DNS 지원</td>
            <td>여러 호스트에 걸친 서비스</td>
        </tr>
        <tr>
            <td>macvlan</td>
            <td>물리 네트워크에 직접 연결</td>
            <td>컨테이너마다 MAC 주소 할당</td>
            <td>물리 네트워크 장비와 직접 통신해야 하는 레거시 앱</td>
        </tr>
    </tbody>
</table>

<br>

### 1.3 네트워크 명령어

<table>
    <thead>
        <th>명령어</th>
        <th>하는 일</th>
    </thead>
    <tbody>
        <tr>
            <td>docker network ls</td>
            <td>네트워크 목록</td>
        </tr>
        <tr>
            <td>docker network create <이름></td>
            <td>네트워크 생성 (기본 드라이버: bridge)</td>
        </tr>
        <tr>
            <td>docker network inspect <이름></td>
            <td>서브넷, 게이트웨이, 연결된 컨테이너 확인</td>
        </tr>
        <tr>
            <td>docker network connect <네트워크><컨테이너></td>
            <td>실행 중인 컨테이너를 네트워크에 추가 연결</td>
        </tr>
        <tr>
            <td>docker network disconnect <네트워크><컨테이너></td>
            <td>연결 해제</td>
        </tr>
        <tr>
            <td>docker network rm <이름></td>
            <td>네트워크 삭제</td>
        </tr>
        <tr>
            <td>docker network prune</td>
            <td>사용하지 않는 네트워크 정리</td>
        </tr>
    </tbody>
</table>

**네트워크도 이미지와 별개인 독립된 자원이다.** 컨테이너와 이미지만 정리하고 네트워크를 그대로 두면 쓰지 않는 네트워크가 계속 쌓인다.

<br>
<hr>

## 2. 기본 bridge와 사용자 정의 bridge ⭐⭐⭐

<br>

Docker를 설치하면 `bridge`, `host`, `none` 네트워크가 기본으로 만들어진다. `--network` 를 지정하지 않으면 컨테이너는 **기본 bridge**(호스트에서는 `docker0` 인터페이스)에 연결된다.

그런데 기본 bridge에 컨테이너 두 개를 띄우고 **이름으로** ping을 보내면 `bad address` 오류가 난다.

<table>
    <thead>
        <th>구분</th>
        <th>기본 bridge</th>
        <th>사용자 정의 bridge</th>
    </thead>
    <tbody>
        <tr>
            <td>만드는 법</td>
            <td>Docker 설치 시 자동 생성</td>
            <td>docker network create 이름</td>
        </tr>
        <tr>
            <td>이름으로 통신</td>
            <td>불가 (하위 호환 때문에 DNS 미제공)</td>
            <td>가능 (Docker 내장 DNS 제공)</td>
        </tr>
        <tr>
            <td>IP로 통신</td>
            <td>가능</td>
            <td>가능</td>
        </tr>
    </tbody>
</table>

IP로는 통신할 수 있지만, **IP는 컨테이너를 재시작하거나 다시 만들 때 바뀔 수 있어** 설정에 적어두기 어렵다. 그래서 **컨테이너끼리 이름으로 통신하려면 사용자 정의 네트워크를 쓴다.**

```bash
# 기본 bridge : 이름으로는 안 된다.
docker run -d --name a1 alpine sleep 3600
docker run -d --name a2 alpine sleep 3600
docker exec a1 ping -c 2 a2         # ping : bad address 'a2'

# 사용자 정의 bridge : 이름으로 된다.
docker network create my-network
docker run -d --name b1 --network my-network alpine sleep 3600
docker run -d --name b2 --network my-network alpine sleep 3600
docker exec b1 ping -c 2 b2         # 응답 OK
```

- **네트워크가 다르면 IP를 알아도 서로 접근할 수 없다.** 기본 bridge의 `a1`은 `my-network`의 `b2` IP로 ping을 보내도 응답을 받지 못한다.
- **컨테이너는 여러 네트워크에 동시에 연결될 수 있다.** `docker network connect my-network a1`을 실행하면 그때부터 `a1`도 `b2`와 통신할 수 있다.

<br>
<hr>

## 3. Compose와 네트워크

`docker compose up`을 하면 Compose가 **`<프로젝트명>_default>`라는 사용자 정의 bridge 네트워크를 자동으로 만들고** 모든 서비스를 거기에 연결한다. 그래서 Compose 안에서는 별도 설정 없이 **서비스 이름(`db`, `redis` 등)으로** 서로 접속할 수 있다.

서비스를 나눠 격리하고 싶으면 `networks:` 로 네트워크를 여러 개 정의한다.

```aiignore
services:
  web:
    networks: [frontend]
  app:
    networks: [frontend, backend]   # 두 네트워크 모두 연결 (중계 역할)
  db:
    networks: [backend]             # backend에만 연결 → web에서는 db에 접근 불가

networks:
  frontend:
  backend:
```

DB를 앞단 웹 서버와 다른 네트워크에 두면, 웹 서버가 뚫려도 DB에 바로 접근할 수 없다.

<br>
<hr>

## 4. 그 밖의 네트워크 드라이버

<br>

### 4.1 host 네트워크 ⭐

**컨테이너가 호스트의 네트워크 인터페이스를 그대로 사용하는 모드다.** 컨테이너가 호스트에서 직접 실행되는 프로세스처럼 동작하므로 **포트 매핑이 필요 없고, `-p` 옵션은 무시된다.**

```bash
docker run -d --network host nginx    # Linux에서는 http://localhost로 바로 접속
```

<table>
    <thead>
        <th>구분</th>
        <th>항목</th>
        <th>설명</th>
    </thead>
    <tbody>
        <tr>
            <td>장점</td>
            <td>성능 향상</td>
            <td>NAT와 가상 브리지를 거치지 않아 지연이 줄어든다.</td>
        </tr>
        <tr>
            <td>장점</td>
            <td>포트 매핑 불필요</td>
            <td>설정이 단순해진다.</td>
        </tr>
        <tr>
            <td>단점</td>
            <td>격리 부족 (보안 위험)</td>
            <td>컨테이너 안의 프로세스가 호스트의 모든 네트워크 인터페이스와 포트에 접근할 수 있다.</td>
        </tr>
        <tr>
            <td>단점</td>
            <td>포트 충돌</td>
            <td>호스트에서 Nginx가 80 포트를 쓰고 있으면 Address already in use로 시작에 실패한다. 같은 이미지로 컨테이너를 여러 개 띄울 수도 없다.</td>
        </tr>
        <tr>
            <td>단점</td>
            <td>멀티 호스트 불가</td>
            <td>단일 호스트에서만 의미가 있다.</td>
        </tr>
        <tr>
            <td>적합한 곳</td>
            <td>성능이 중요한 서비스</td>
            <td>로그 수집 에이전트, 모니터링 에이전트(Node Exporter), 고성능 프록시</td>
        </tr>
    </tbody>
</table>

host 모드로 띄우기 전에는 **호스트에서 그 포트를 이미 쓰고 있는지** 확인한다.

```bash
# Linux
sudo ss -tulnp | grep :80       # 또는 sudo netstat -tulnp | grep :80

# macOS
sudo lsof -iTCP:80 -sTCP:LISTEN
```

> macOS와 Windows의 Docker Desktop은 컨테이너를 내부 Linux VM에서 실행하므로, host 모드의 "호스트"가 내 PC가 아니라 그 VM이 된다. 위 동작은 Linux 기준이다. 

<br>

## 4.2 none 네트워크

**컨테이너를 어떤 네트워크에도 연결하지 않는 모드이다.** 컨테이너 안에는 자기 자신을 가리키는 루프백(`lo`)만 있고, 외부에는 통신이 완전히 차단된다. DNS도 동작하지 않는다.

```bash
docker run --rm --network none alpine ip addr   # lo 하나만 보인다.
```

<br>

## 4.3 overlay, macvlan
- **overlay :** 여러 Docker 호스트에 걸쳐 있는 컨테이너들이 같은 네트워크에 있는 것처럼 통신하게 해준다. 자세한 내용은 Kubernetes에서 다룬다.
- **macvlan :** 컨테이너마다 고유한 **MAC** 주소를 할당해, 컨테이너가 물리 네트워크에 직접 연결된 장비처럼 보이게 한다.

<br>
<hr>

## 5. 데이터 관리와 볼륨

<br>

### 5.1 왜 볼륨이 필요한가

컨테이너 안에서 만든 파일은 **컨테이너의 쓰기 레이어**에 저장되므로 컨테이너를 삭제하면 함께 사라진다. MySQL 컨테이너를 업데이트하려고 지웠다가 다시 만들면 **DB 데이터가 모두 사라진다.**

볼륨은 데이터를 **컨테이너 밖(Docker가 관리하는 호스트 영역)** 에 저장해서, 컨테이너를 삭제해도 데이터가 남게 한다. 호텔 객실은 체크아웃하면 정리되지만, 프런트에 맡긴 귀중품은 다음에 와도 찾아갈 수 있는 것과 같다.

<br>

## 5.2 스토리지 유형 4가지

<table>
    <thead>
        <th>유형</th>
        <th>저장 위치</th>
        <th>데이터 지속성</th>
        <th>성능</th>
        <th>사용 사례</th>
    </thead>
    <tbody>
        <tr>
            <td>Container Storage</td>
            <td>컨테이너 쓰기 레이어</td>
            <td>컨테이너와 함께 삭제</td>
            <td>Copy-on-Write라 쓰기가 많으면 느림</td>
            <td>테스트, 임시 데이터</td>
        </tr>
        <tr>
            <td>Bind Mount</td>
            <td>내가 지정한 호스트 경로</td>
            <td>호스트 파일이 남아 있는한 유지</td>
            <td>호스트 파일 시스템 속도 (macOS/Windows는 상대적으로 느림)</td>
            <td>개발 중 소스 공유, 설정 파일 주입</td>
        </tr>
        <tr>
            <td>Volume</td>
            <td>Docker가 관리하는 호스트 영역</td>
            <td>컨테이너와 독립적으로 유지</td>
            <td>호스트 디스크에 가까움</td>
            <td>운영 데이터, DB</td>
        </tr>
        <tr>
            <td>tmpfs Mount</td>
            <td>메모리(RAM)</td>
            <td>컨테이너가 멈추면 사라짐</td>
            <td>매우 빠름</td>
            <td>민감한 임시 정보, 임시 캐시</td>
        </tr>
    </tbody>
</table>

- **Container Storage :** 이미지 레이어의 파일이 고치면 그 파일을 쓰기 레이어로 먼저 복사한 뒤 수정한다(Copy-on-Write). 큰 파일을 자주 쓰는 작업에는 맞지 않는다.
- **운영 환경에서는 주로 Volume을 쓴다.** 데이터가 컨테이너와 독립으로 남고, 호스트의 특정 경로 구조에 의존하지 않아 다른 서버로 옮기기도 한다.

<br>

## 5.3 Volume

`-v 이름:컨테이너 경로`로 마운트한다. **앞자리가 이름이면 Volume, `/`나 `./`로 시작하는 경로면 Bind Mount** 가 된다.

```bash
docker volume create mysql-data         # 미리 만들기 (없는 이름을 쓰면 자동 생성)
docker volume ls
docker volume inspect mysql-data        # Mountpoint = 실제 저장 위치

docker run -d --name mysql1 \
  -e MYSQL_ROOT_PASSWORD=password \
  -v mysql-data:/var/lib/mysql  \
  mysql:8.0
  
docker rm -f mysql1                     # 컨테이너 삭제
docker run -d --name mysql2 \
  -e MYSQL_ROOT_PASSWORD=password \
  -v mysql-data:/var/lib/mysql \
  mysql:8.0                             # 같은 볼륨 → 이전 데이터가 그대로 보인다.
```

MySQL의 데이터 디렉터리(`/var/lib/mysql`)를 볼륨으로 두면 컨테이너를 다시 만들어도 DB 데이터가 유지된다.

<br>

## 5.4 Bind Mount

호스트의 디렉터리를 컨테이너에 그대로 마운트한다. 호스트와 컨테이너가 **같은 파일을 실시간으로 공유**하므로, 한쪽에서 바꾸면 다른 쪽에서 바로 보인다.

```bash
# 내 PC의 html 폴더를 nginx 문서 폴더로 공유
docker run -d -p 8080:80 -v $(pwd)/html:/usr/share/nginx/html nginx

# :ro → 일기 전용. 컨테이너는 파일을 수정할 수 없다. (설정 파일 주입용)
docker run -d -p 8080:80 -v $(pwd)/nginx.conf:/etc/nginx/nginx.conf:ro nginx
```

<br>

## 5.5 Volume vs Bind Mount ⭐

<table>
    <thead>
        <th>특징</th>
        <th>Volume</th>
        <th>Bind Mount</th>
    </thead>
    <tbody>
        <tr>
            <td>저장 위치</td>
            <td>/var/lib/docker/volumes/ (Docker가 관리)</td>
            <td>내가 지정한 호스트 디렉터리</td>
        </tr>
        <tr>
            <td>관리</td>
            <td>docker volume create/ls/rm/prune</td>
            <td>일반 파일, 폴더처럼 관리</td>
        </tr>
        <tr>
            <td>독립성</td>
            <td>호스트 경로 구조와 무관 (운영에 적합)</td>
            <td>호스트 경로 구조에 의존 (로컬 개발에 적합)</td>
        </tr>
        <tr>
            <td>초기 데이터</td>
            <td>빈 볼륨을 마운트하면 이미지 안의 기존 파일이 볼륨으로 복사됨</td>
            <td>호스트 디렉터리가 컨테이너 경로를 그대로 덮어씀</td>
        </tr>
        <tr>
            <td>권한</td>
            <td>Docker가 관리</td>
            <td>호스트 파일 권한(UID/GID)을 직접 고려해야 함</td>
        </tr>
        <tr>
            <td>예시</td>
            <td>DB, 업로드 파일</td>
            <td>소스 코드, 설정 파일, 로그 확인</td>
        </tr>
    </tbody>
</table>

판단 기준은 **"이 파일을 내가 호스트에서 직접 열어 볼 일이 있는가?"** 다. 있다면 Bind Mount, 없다면 Volume이 기본 선택이다.

"초기 데이터" 차이는 실수하기 쉽다. nginx 문서 폴더에 **빈 Volume**을 붙이면 기본 `index.html`이 볼륨으로 복사되어 그대로 보이지만, **빈 호스트 폴더를 Bind Mount**하면 그 경로가 빈 폴더로 덮여 403이 난다.

<br>

### 5.6 tmpfs Mount

**데이터를 디스크 대신 메모리(RAM)에 저장하는 방식이다.**

- 디스크 I/O가 없어 빠르고, 데이터가 디스크에 남지 않아 **민감한 데이터의 유출 위험**을 줄인다.
- 컨테이너가 멈추면 데이터는 모두 사라진다.
- 쓴 만큼 메모리를 쓰므로 **`tmpfs-size`로 상한**을 정한다.
- Linux 전용 기능이지만, Docker Desktop도 내부 Linux VM에서 컨테이너를 돌리므로 macOS, Windows에서도 실습할 수 있다.

```bash
docker run -d --mount type=tmpfs, destination=/app/tmp,tmpfs-size=64m my-app
# 짧게 쓰면: --tmpfs /app/tmp:size=64m
```

사용 시나리오: 임시 파일 처리, 고성능 작업, 민감한 데이터 처리, 읽기 전용 컨테이너의 쓰기 공간

<br>
<hr>

## 6. 컨테이너 보안과 자원 제한

컨테이너는 **호스트 커널을 공유**하므로 VM보다 격리가 약하다. 그래서 **컨테이너가 할 수 있는 일을 최소한으로 줄이는 것**이 보안의 기본이고, 컨테이너 하나가 서버 자원을 다 차지하지 않도록 **자원 사용량을 제한**해야 한다. 

<table>
    <thead>
        <th>옵션</th>
        <th>막는 것</th>
    </thead>
    <tbody>
        <tr>
            <td>--user 1000:1000 / Dockerfile USER</td>
            <td>root로 실행되는 것</td>
        </tr>
        <tr>
            <td>--cap-drop All (--cap-add로 필요한 것만)</td>
            <td>불필요한 root 권한 (Capability)</td>
        </tr>
        <tr>
            <td>--read-only</td>
            <td>파일 시스템 변조 (악성 파일 설치, 실행 파일 교체)</td>
        </tr>
        <tr>
            <td>--security-opt no-new-privileges</td>
            <td>sudo, setuid로 권한을 올리는 것</td>
        </tr>
        <tr>
            <td>--memory, --cpus</td>
            <td>컨테이너 하나가 서버 자원을 독차지 하는 것</td>
        </tr>
    </tbody>
</table>

<br>

### 6.1 root가 아닌 사용자로 실행

컨테이너는 기본적으로 root(`uid=0`)로 실행된다. `--user 1000:1000`처럼 지정하면 일반 사용자로 실행되어 `/etc/passwd`같은 시스템 파일을 고치려 해도 `Permission denied`가 난다.

가장 좋은 방법은 **Dockerfile에 `USER`를 지정해 이미지 자체가 일반 사용자로 실행되게 하는 것**이다. `--user`는 실행할 때마다 적어야 해서 빠뜨리기 쉽다.

<br>

### 6.2 불필요한 권한 제거 (참고: 인프라 영역)

리눅스는 root 권한을 **Capability**라는 작은 단위로 나눈다. (예: `CROWN` 파일 소유자 변경, `NET_BIND_SERVICE` 1024 미만 포트 사용, `NET_RAW` raw 소켓 사용) Docker는 기본으로 일부 Capability를 허용하는데, `--cap-drop` 으로 필요 없는 것을 제거할 수 있다. 만능 열쇠 대신 필요한 방 열쇠만 나눠 주는 것과 같다.

<br>

### 6.3 읽기 전용 파일 시스템과 권한 상승 방지

- `--read-only` : 공격자가 침투해도 악성 파일을 설치하거나 실행 파일을 바꿀 수 없다. 앱이 꼭 써야 하는 경로만 tmpfs나 볼륨으로 열어 준다.
- `--security-opt no-new-privileges` : 프로세스가 sudo, setuid 프로그램 등으로 더 높은 권한을 얻지 못하게 한다. `--user`로 일반 사용자로 실행해도 이미지 안에 setuid 프로그램이 있으면 그걸로 root가 될 수 있어 함께 쓴다.

<br>

### 6.4 리소스 제한

제한하지 않으면 컨테이너 하나가 메모리 누수나 무한 루프로 **서버 전체 자원을 다 써버릴 수 있고,** 같은 서버의 다른 컨테이너까지 느려지거나 죽는다.

```bash
docker run -d --memory 512m --cpus 1.0 my-app    # 메모리 512MB, CPU 1코어까지
docker stats                                    # 컨테이너별 실제 사용량 확인
```

<br>

### 6.5 한 번에 적용한 예

```bash
docker run -d --name secure-app \
  --user 1000:1000 \
  --cap-drop ALL \
  --read-only \
  --tmpfs /tmp:size=64m \
  --security-opt no-new-privileges \
  --memory 512m --cpus 1.0 \
  -p 8080:8080 \
  my-app:1.0.0
```

<br>
<hr>

## 7. 핵심 정리

- 호스트에서 컨테이너로 들어올 때만 `-p`가 필요하고, **컨테이너끼리는 같은 네트워크에서 `이름:컨테이너포트`** 로 통신한다.
- **기본 bridge는 이름 DNS를 제공하지 않는다.** 이름으로 통신하려면 `docker network create`로 만든 사용자 정의 bridge를 쓴다. Compose는 이걸 자동으로 만들어 준다.
- 네트워크가 다르면 IP를 알아도 통신할 수 없다. Compose에서 네트워크를 나누면 **DB를 앞단과 격리**할 수 있다.
- host 네트워크는 빠르고 포트 매핑이 없지만, **격리가 없고 포트 충돌**이 난다.
- 컨테이너 쓰기 레이어의 데이터는 컨테이너와 함께 사라진다. **운영 데이터는 Volume**, 호스트에서 직접 볼 파일은 **Bind Mount**, 디스크에 남기면 안 되는 임시 데이터는 **tmpfs.**
- 빈 Volume은 이미지의 기존 파일을 **복사해 오고,** Bind Mount는 컨테이너 경로를 **덮어쓴다.**
- 보안은 **non-root, Capability 제거, 읽기 전용, 권한 상승 방지**로 컨테이너가 할 수 있는 일을 줄이고, `--memory` · `--cpus`로 자원을 제한한다.

<br>

