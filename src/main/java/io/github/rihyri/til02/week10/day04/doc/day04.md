<br>

# Week 10 - Day 04. 컨테이너 로그와 리소스 모니터링

<br>

이번 수업은 **컨테이너 로그가 어디에 어떻게 쌓이는지(로그 드라이버) → 로그를 골라 보는 방법(스트림, 시간 범위) → 로그가 디스크를 채우지 않게 하는 방법(로테이션) → CPU·메모리 사용량 확인(docker status) → Docker가 쓰는 디스크 정리(prune)** 순서로 진행됐다. 그 중 **로그 로테이션과 디스크 정리가 특히 중요하다.** 둘 다 놓치면 서버 디스크가 가득 차서 서비스 전체가 멈춘다.

<br>
<hr>

## 1. 컨테이너 로그가 쌓이는 곳

<br>

### 1.1 로그가 만들어지는 경로

```azure
 메인 프로세스 (PID 1)                     docker exec 로 띄운 프로세스
 Dockerfile 의 CMD / ENTRYPOINT            출력은 내 터미널로만 전달
 예: java -jar app.jar                     → docker logs 에 남지 않음
        │
        │ stdout / stderr
        ▼
 Docker Daemon (dockerd) : 두 스트림을 받아 로그 드라이버로 전달
        │
        ▼
 ◇ 로그 드라이버 ◇
   ├─ json-file (기본값) : 호스트의 <ID>-json.log ──(파일을 읽어 출력)──▶ docker logs
   ├─ syslog             : 외부 syslog 서버 (udp/tcp 514) ──(로컬 캐시)──▶ docker logs
   └─ fluentd / awslogs / gelf : ELK, CloudWatch 같은 로그 수집 시스템
```

- Docker는 **컨테이너 메인 프로세스(PID1)의 stdout과 stderr를 가로채서 로그 드라이버에 넘긴다.** 메인 프로세스는 Dockerfile의 `CMD`나 `ENTRYPOINT`로 실행된 프로세스다.
- **로그 드라이버**는 받을 로그들을 어디에, 어떤 형식으로 저장할지 정하는 부품이다. 우체국에서 편지를 받아 배송지별로 분류하는 창구와 같다. 따로 정하지 않으면 `json-file`이 쓰인다.
- `json-file`은 로그를 호스트의 JSON 파일에 한 줄씩 기록하고, `docker logs`는 이 파일을 읽어서 보여준다. 즉 **`docker logs`는 컨테이너 안을 들여다보는 명령이 아니라 호스트에 저장된 로그 파일을 읽는 명령이다.**
- `docker exec`로 실행한 명령은 PID 1이 아닌 별도 프로세스라서, 그 출력은 내 터미널로만 오고 로그에는 남지 않는다.

<br>

### 1.2 로그는 파일이 아닌 표준 출력으로

컨테이너 애플리케이션은 **로그를 파일이 아니라 stdout/stderr로 출력하는 것이 원칙이다.** ("로크는 이벤트 스트림으로 다룬다") 앱은 출력만 하고, 저장과 전송은 실행 환경이 맡는다.

앱이 컨테이너 안의 파일(예: `/app/logs/app.log`)에 로그를 쓰면 다음 문제가 생긴다.

- `docker logs`로 볼 수 없어 매번 `docker exec`로 들어가야 한다.
- 로그가 컨테이너 쓰기 레이어에 쌓여 컨테이너 크기가 계속 커진다.
- 컨테이너를 삭제하면 로그도 함께 사라진다.

<table>
	<thead>
		<tr>
			<th>예</th>
			<th>동작</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td>Spring Boot 기본 로깅</td>
			<td>콘솔(stdout)로 출력 → 설정 없이 docker logs로 확인 가능</td>
		</tr>
        <tr>
			<td>Spring Boot + logging.file.name</td>
			<td>파일로 저장 → docker logs에는 아무것도 안 보인다.</td>
		</tr>
        <tr>
			<td>nginx 공식 이미지</td>
			<td>access.log → /dev/stdout, error.log → /dev/stderr로 심볼릭 링크</td>
		</tr>
	</tbody>
</table>

nginx 방식은 파일에만 쓰는 프로그램을 컨테이너에 넣을 때 자주 쓰는 방법이다.

<br>

## 1.3 로그 드라이버와 로그 파일

```bash
# 컨테이너가 사용하는 로그 드라이버와 로그 파일 위치
docker inspect <컨테이너> --format '{{.HostConfig.LogConfig.Type}}'
docker inspect <컨테이너> --format '{{.LogPath}}'

# Docker 데몬의 기본 로그 드라이버
docker info --format '{{.LoggingDriver}}'
```

json-file 로그 파일을 열어 보면 출력 한 줄이 JSON 한 줄로 저장되어 있다.

```json
{"log":"1: stdout log\n","stream":"stdout","time":"2026-11-01T01:02:03.123456789Z"}
{"log":"1: stderr log\n","stream":"stderr","time":"2026-11-01T01:02:03.123498765Z"}
```

어느 스트림에서 나왔는지 (`stream`)와 Docker가 받은 시각(`time`, UTC)이 함께 기록되므로, **실제 로그 파일은 앱이 출력한 글자 수보다 2~3배 커진다.**

<table>
	<thead>
		<tr>
			<th>로그 드라이버</th>
			<th>저장 위치</th>
            <th>docker logs</th>
			<th>특징</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td>json-file</td>
			<td>호스트의 JSON 파일</td>
            <td>가능</td>
			<td>기본값. 크기 제한이 없어 로테이션 설정 필요</td>
		</tr>
        <tr>
			<td>local</td>
			<td>호스트의 Docker 전용 형식 파일</td>
            <td>가능</td>
			<td>로테이션과 압축이 기본 적용되어 디스크를 적게 씀</td>
		</tr>
        <tr>
			<td>fluentd/gelf/awslogs</td>
			<td>외부 로그 수집 시스템</td>
            <td>가능 (로컬 캐시)</td>
			<td>ELK, Graylog, CloudWatch 연동</td>
		</tr>
        <tr>
			<td>none</td>
			<td>저장하지 않음</td>
            <td>불가</td>
			<td>로그가 전혀 필요 없는 컨테이너</td>
		</tr>
	</tbody>
</table>

<br>
<hr>

## 2. 로그 골라보기

<br>

### 2.1 표준 출력과 표준 에러

리눅스 프로세스는 세 개의 통로(스트림)을 가지고 시작한다.

<table>
	<thead>
		<tr>
			<th>번호</th>
			<th>스트림</th>
            <th>용도</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td>0</td>
			<td>stdin</td>
            <td>표준 입력</td>
		</tr>
        <tr>
			<td>1</td>
			<td>stdout</td>
            <td>일반 실행 결과</td>
		</tr>
        <tr>
			<td>2</td>
			<td>stderr</td>
            <td>오류, 경고 메시지</td>
		</tr>
	</tbody>
</table>

Docker도 두 스트림을 구분해서 저장하고, `docker logs`는 **컨테이너의 stdout은 내 터미널의 stdout으로, stderr는 내 터미널의 stderr**로 그대로 전달한다. 그래서 리다이렉션으로 한 쪽만 걸러볼 수 있다.

```bash
docker run --name stream-test alpine sh -c 'echo "stdout log"; echo "stderr log" >&2'
#     >&2 : 이 출력을 2번 통로(stderr)로 보낸다.

docker logs stream-test 2>/dev/null      # stdout 만
docker logs stream-test 1>/dev/null      # stderr 만
#     /dev/null : 들어온 데이터를 모두 버리는 특수 파일 ("블랙홀")
```

<br>

### 2.2 `docker logs | grep`에서 에러가 안 보이는 이유 ⭐

**파이프(`|`)는 stdout만 다음 명령으로 넘긴다.** 그래서 stderr로 나온 에러는 `grep`에 도달하지 못한다. 분명히 있는 에러가 검색되지 않는 실무 상황이 바로 이것이다.

```bash
docker logs app | grep ERROR            # stderr 로 나온 에러는 못 찾는다.
docker logs app 2>&1 | grep ERROR       # stderr 를 stdout 에 합친 뒤 검색
```

**종료(Exited)된 컨테이너도 삭제하기 전까지 로그 파일이 남아 있어** `docker logs`로 볼 수 있다. 장애 원인을 찾을 때 매우 중요하다. 반대로 `--rm`으로 띄운 컨테이너는 종료와 함께 로그도 사라진다.

<br>

### 2.3 실시간 로그와 시간 범위

<table>
	<thead>
		<tr>
			<th>옵션</th>
			<th>설명</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td>-f, --follow</td>
			<td>새로 쌓이는 로그를 실시간으로 따라간다. (tail -f와 같음)</td>
		</tr>
        <tr>
			<td>--tail N</td>
			<td>마지막 N줄만 본다.</td>
		</tr>
        <tr>
			<td>--t, --timestamps</td>
			<td>Docker가 로그를 받은 시간을 붙인다. 항상 UTC라 KST보다 9시간 느리게 보인다.</td>
		</tr>
        <tr>
			<td>--since, --until</td>
			<td>10m, 1h 같은 상대 시간이나 날짜 시각으로 범위를 자른다.</td>
		</tr>
	</tbody>
</table>

```bash
docker logs --tail 10 -f app            # 최근 10줄부터 이어서 실시간으로
docker logs -t --since 10m app          # 최근 10분, 시각 포함

# "어제 오후 3시쯤 에러가 났다." → 그 시간대만 잘라 본다.
docker logs --since "2026-11-18T14:50:00" --until "2026-11-18T15:10:00" app 2>&1 | grep -i error 
```

- 몇 달 실행된 컨테이너에 `-f` 만 쓰면 수십만 줄이 한꺼번에 출력되어 터미널이 멈춘 것처럼 보인다. **`-tail`과 함께 쓰는 습관**을 들인다.
- -`f` 중 `Ctrl+C`는 **로그 보기만 멈출 뿐 컨테이너를 멈추지 않는다.** (`-d` 없이 `docker run` 했을 때의 `Ctrl+C`와 다르다.)
- 시간대를 붙이지 않은 날짜 시간은 **명령을 실행한 내 PC의 시간대**로 해석된다. 위 예시는 KST로 해석된다.

<br>
<hr>

## 3. 로그 로테이션 ⭐⭐

<br>

### 3.1 왜 필요한가

**json-file 드라이버는 기본적으로 로그 파일 크기를 제한하지 않는다.** 로그를 많이 쓰는 컨테이너를 몇 주, 몇 달 실행하면 로그 파일 하나가 수십 GB가 되어 **서버 디스크가 가득 찬다.** 그러면 DB 쓰기, 다른 컨테이너의 로그 기록, 새 컨테이너 생성까지 모두 실패해 로그 하나 때문에 서비스 전체가 멈출 수 있다.

<br>

### 3.2 `max-size`와 `max-file`

파일 하나의 최대 크기와 보관할 파일 개수를 제한하는 것을 **로그 로테이션**이라고 한다. 저장 공간이 차면 가장 오래된 영상부터 지우고 새로 녹화하는 CCTV와 같다.

```bash
docker run -d --name app \
  --log-opt max-size=10m \
  --log-opt max-file=3 \
  my-app:1.0.0
```

```azure
<ID>-json.log     ← 현재 기록 중. 10MB 가 되면 닫히고 .1 로 밀려난다
<ID>-json.log.1
<ID>-json.log.2   ← max-file 을 넘으면 가장 오래된 파일부터 삭제
→ 최대 max-size × max-file = 10MB × 3 = 약 30MB
```

- `max-size`에는 `k`, `m`, `g` 단위를 붙인다. `max-file`은 **`max-size`를 함께 지정했을 때만** 의미가 있다.
- 로테이션으로 밀려난 파일은 삭제되므로 **`docker logs`로 볼 수 있는 범위도 그만큼 줄어든다.** 오래된 로그가 꼭 필요하면 fluentd, awslogs 같은 로그 드라이버나 Fluent Bit, Filebeat 같은 수집 에이전트로 외부 시스템에 보낸다.
- 크기를 너무 작게 잡으면 **장애 직전 로그가 이미 지워져** 원인을 못 찾는다. 서비스의 하루 로그 양을 보고 **적어도 며칠 치는 남도록** 정한다.

