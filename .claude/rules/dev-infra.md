# 로컬 dev 인프라

> **범위**: 로컬에서 app 모듈을 띄울 때 사용하는 외부 의존성 (Postgres, Redis, Pinpoint APM) 의 Docker Compose 셋업과 환경변수 규약. Flyway / CI/CD 는 본 문서 범위 외 (별도 티켓).

## 구성

| 서비스 | 이미지 | 포트 | 볼륨 |
|--------|--------|------|------|
| Postgres | `postgres:16` | `5432:5432` | `postgres_data` (named) |
| Redis | `redis:7-alpine` | `6379:6379` | `redis_data` (named) |

- 이미지 버전은 `compose.yaml` 에 직접 픽스. `.env` 로 빼지 않는다 — 인프라 버전은 PR 리뷰 대상이다.
- 두 서비스 모두 `healthcheck` 가 있어 `docker compose ps` 의 `STATUS` 컬럼으로 준비 여부를 확인할 수 있다.
- `name: crispin-lab` 으로 compose project name 을 고정 — worktree 와 무관하게 동일 프로젝트로 인식되어, 작업 브랜치를 바꿔도 같은 볼륨을 공유한다.

## 최초 셋업

```bash
cp .env.example .env             # 환경변수 파일 생성 (값은 default 그대로 사용 가능)
docker compose up -d             # postgres + redis 백그라운드 기동
docker compose ps                # 두 서비스 모두 healthy 인지 확인
set -a && source .env && set +a  # 현재 셸에 .env 값을 export — bootRun 의 placeholder 해석에 필요
./gradlew :app:bootRun           # app 부팅
```

`.env` 는 `.gitignore` 처리되어 있으니 로컬 값을 자유롭게 바꿔도 된다. `.env.example` 만 추적된다.

`.env` 가 없으면 `docker compose up -d` 가 `POSTGRES_DB / USER / PASSWORD` 미정의로 즉시 fail-fast 한다 — silent 사고를 피하기 위한 의도적 정책.

### IntelliJ `bootRun` 에 `.env` 주입

`docker compose` 는 루트 `.env` 를 자동 로드하지만, **Gradle / Spring `bootRun` 은 OS 환경변수만 읽는다** — `application.yml` 의 `${POSTGRES_USER}` 등이 resolve 되지 않으면 부팅이 즉시 실패한다. IntelliJ 에서 띄울 때는 [EnvFile 플러그인](https://plugins.jetbrains.com/plugin/7861-envfile) 으로 `.env` 를 Run Configuration 에 주입한다.

1. IntelliJ Marketplace 에서 `EnvFile` 설치 후 IDE 재시작.
2. `Run → Edit Configurations…` → Gradle/Spring Boot Run Configuration 선택.
3. `EnvFile` 탭에서 `Enable EnvFile` 체크 → `+` 로 루트 `.env` 추가.
4. Apply 후 실행 — `.env` 의 값이 환경변수로 주입되어 `application.yml` placeholder 가 resolve 된다.

`.env` 를 갱신하면 Run Configuration 재실행만으로 반영된다. 셸에서 직접 띄울 때는 `set -a && source .env && set +a && ./gradlew :app:bootRun`.

## 환경변수

`.env` (또는 OS 환경변수) 가 정의하는 키:

| 키 | default (`.env.example`) | 용도 |
|----|--------------------------|------|
| `POSTGRES_DB` | `lab` | Postgres 데이터베이스 이름 |
| `POSTGRES_USER` | `lab` | Postgres 사용자명 |
| `POSTGRES_PASSWORD` | `lab` | Postgres 비밀번호 (로컬 전용) |

`app/src/main/resources/application.yml` 의 datasource:

- `POSTGRES_USER` / `POSTGRES_PASSWORD` — **fallback 없음**. 미설정 시 Spring 이 `Could not resolve placeholder` 로 즉시 실패. 운영에서 자격증명 누락 시 dev default 로 silently 붙는 사고를 막는다.
- `POSTGRES_HOST` (default `localhost`) / `POSTGRES_PORT` (default `5432`) / `POSTGRES_DB` (default `lab`) — fallback 있음. 컨테이너로 띄울 때만 host 를 `host.docker.internal` 또는 compose network 의 서비스명으로 override.

Redis 는 현재 인증/포트 옵션이 모두 default 라 `.env` 키가 없다. AUTH 또는 다른 인스턴스 분리가 필요해지면 그때 `REDIS_*` 키를 도입한다.

## 자주 쓰는 명령

```bash
docker compose ps             # 상태 확인
docker compose logs -f postgres
docker compose stop           # 컨테이너 정지 (볼륨/데이터 유지)
docker compose down           # 컨테이너 제거 (볼륨/데이터 유지)
docker compose down -v        # 컨테이너 + 볼륨 제거 (DB 데이터 완전 삭제)
```

볼륨이 살아 있으면 `docker compose up -d` 재기동 시 이전 데이터가 그대로 복구된다. `restart: unless-stopped` 정책이 박혀 있어 호스트 재부팅 시 자동 기동.

## `docker/postgres/` 디렉토리

초기 SQL/스키마 부트스트랩 자리. 사용 시 `compose.yaml` 의 postgres 서비스 volumes 에 다음 한 줄을 추가한다:

```yaml
- ./docker/postgres/initdb:/docker-entrypoint-initdb.d:ro
```

`*.sql` / `*.sh` 가 컨테이너 최초 기동 시 1회 실행된다. 본 티켓에서는 자리만 마련하고 마운트는 켜지 않는다 — Flyway 도입(별도 티켓) 전까지의 임시 hook 용.

## Dockerfile (배포용)

`app/Dockerfile` 은 멀티스테이지 (`eclipse-temurin:21-jdk-jammy` 빌드 → `21-jre-jammy` 런타임) 빌드를 정의한다. 본 환경의 jvmToolchain (21) 과 정합. builder 단계는 의존 그래프 (build.gradle.kts) 만 먼저 복사해 dependency resolution 을 캐싱 — 소스만 바뀐 빌드는 재의존해소 없이 빠르게 끝난다.

로컬 dev 에서는 보통 `bootRun` 으로 충분하다 — Dockerfile 은 배포 표준이 사전에 정의되어 있어야 해서 함께 포함되어 있다.

### 빌드·실행

```bash
docker build -f app/Dockerfile -t crispin-lab:dev .
docker run --rm \
    -e POSTGRES_HOST=host.docker.internal \
    -e POSTGRES_USER=lab \
    -e POSTGRES_PASSWORD=lab \
    -p 8080:8080 \
    crispin-lab:dev
```

- macOS / Windows Docker Desktop 에서 `host.docker.internal` 이 호스트 Postgres (compose) 를 가리킨다. Linux 에서는 `--add-host=host.docker.internal:host-gateway` 또는 `--network host`.
- compose network 안에서 app 을 같이 띄우는 시나리오는 별도 티켓 — `compose.yaml` 에 app 서비스 추가 + `POSTGRES_HOST=postgres` 로 묶는다.

### Follow-up (별도 티켓 후보)

- PID 1 신호 전파 / JVM heap 옵션 / actuator readiness probe — 운영 배포 PR 에서 같이 다룬다.
- 컨테이너 healthcheck (`HEALTHCHECK` 지시문) — actuator endpoint 도입 시점.

## 회귀 방지

- 테스트는 Testcontainers 가 띄우는 Postgres 컨테이너에서 동작한다 — 로컬 호스트의 `compose.yaml` Postgres 가 떠 있지 않아도 `./gradlew test` 가 통과한다 (`migration.md` 참조). `@SpringBootTest` 는 `@Import(TestcontainersConfig::class)` 로 `@ServiceConnection PostgreSQLContainer<*>` 빈을 받아 datasource 가 자동 wiring 된다.
- `app/src/test/resources/application.yml` 의 datasource 값은 `@ServiceConnection` 이 런타임에 override 한다 — 본 placeholder 는 main yml 의 `${POSTGRES_*}` resolution 실패만 막기 위한 stub.
- `compose.yaml` 의 service 이름 / 환경변수 키를 바꾸면 `application.yml` 의 placeholder 와 본 문서의 표가 같이 갱신되어야 한다.

## Pinpoint APM (로컬 dev)

Pinpoint 풀스택(HBase + MySQL + collector + web + Kafka + Pinot 4종 + init 2종 = 11개 컨테이너) 은 매일 띄울 만큼 가볍지 않아 Docker Compose `profiles` 로 분리한다. 평소 `docker compose up -d` 는 그대로 postgres + redis 만 띄우고, `--profile pinpoint` 를 명시할 때만 APM 스택이 함께 기동. **Docker Desktop 메모리는 16GB 이상 권장** (Pinot/HBase JVM 으로 ~6GB 사용).

### 구성

| 그룹 | 서비스 | 역할 |
|------|--------|------|
| Pinpoint core | `pinpoint-hbase` | trace / 메타 저장. standalone 모드 (단일 JVM 안에 master + regionserver + 임베디드 ZK) |
| | `pinpoint-mysql` | pinpoint-web 의 user/role/dashboard 메타 (이미지가 MySQL JDBC URL hardcoded — 교체 불가) |
| | `pinpoint-collector` | agent 트래픽 수집. `:3.0.5-metric` 이미지 (Pinot 발행 모듈 포함) |
| | `pinpoint-web` | UI + REST. `:3.0.5-metric` 이미지 (`/api/inspector/*` controller 포함) |
| Inspector / metric backend | `pinpoint-kafka` | inspector-stat / system-metric / exception-trace 토픽 |
| | `pinpoint-kafka-init` | 토픽 7종 생성 (one-shot, 자동 종료) |
| | `pinot-zoo` | Pinot 클러스터 ZK (HBase 임베디드 ZK 와 별개) |
| | `pinot-controller` | Pinot 제어 + UI (port 9000) |
| | `pinot-broker` | 쿼리 라우팅 |
| | `pinot-server` | 데이터 저장 + 쿼리 실행 |
| | `pinot-init` | 7개 Pinot 테이블 schema/config 등록 (one-shot, 자동 종료) |

본 셋업의 복잡도 대부분은 **로컬 dev** 가 아니라 **Apple Silicon Rosetta x86 에뮬레이션** 때문. x86 Linux 호스트에서는 `pinpoint-hbase` 의 default distributed + 외부 ZK quorum 구성이 그대로 동작 (standalone override 불필요). 운영 / CI 환경에서는 본 우회를 그대로 옮기지 말 것.

`CLUSTER_ENABLE=false` 로 두어 collector / web 의 Redis cluster coordination 의존을 제거함 (단일 인스턴스 dev 에서 의미 없는 publish/subscribe). 다중 collector / web 도입 시 `CLUSTER_ENABLE=true` 로 켜고 `pinpoint-redis` 서비스 + `SPRING_DATA_REDIS_*` 환경변수를 다시 추가. realtime active thread dump 같은 cluster-기반 기능도 그때 살아남.

### 버전 픽스

- `compose.yaml` 의 Pinpoint 이미지 태그는 `${PINPOINT_VERSION:-3.0.5}` 로 default 픽스. collector / web 은 `${PINPOINT_VERSION}-metric` 로 metric variant 사용. agent jar 도 동일 버전을 다운로드.
- 3.0.0 ~ 3.0.3 은 github releases 에 agent tarball asset 이 첨부되어 있지 않다 (3.0.4 부터 첨부). 따라서 본 프로젝트는 3.0.4 미만으로 내릴 수 없다.
- Pinot 은 `apachepinot/pinot:1.0.0-11-amazoncorretto` 픽스. Kafka 는 `ubuntu/kafka:3.1-22.04_beta` 픽스.
- 운영 환경 배포는 본 문서 범위 외 — 별도 티켓에서 다룬다.

### 사전 준비 — agent jar + mysql init SQL 다운로드

```bash
./docker/pinpoint/agent/download.sh           # pinpoint-bootstrap-<ver>.jar
./docker/pinpoint/mysql-init/download.sh      # pinpoint-web 의 schema SQL 2건
# PINPOINT_VERSION=3.0.4 ./docker/pinpoint/agent/download.sh   # 다른 버전이 필요할 때
```

두 스크립트 모두 **로컬에 결과물을 commit 하지 않는다** — 버전 종속이라 git 추적 대상이 아니다. `docker/pinpoint/agent/.gitignore` 와 `docker/pinpoint/mysql-init/.gitignore` 가 산출물을 ignore 처리한다.

`mysql-init/download.sh` 는 `pinpoint-mysql` 컨테이너의 `/docker-entrypoint-initdb.d` 에 마운트되는 SQL 두 파일을 `raw.githubusercontent.com` 에서 한 번 받는다. mysql 컨테이너는 빈 데이터 디렉토리 (첫 boot) 일 때만 이 SQL 을 실행한다 — 재기동 시점에 외부 네트워크를 다시 타지 않는다. 사내 폐쇄망 등 raw.github 접근이 불가능한 환경이라면 별도 채널로 SQL 두 파일을 받아 `docker/pinpoint/mysql-init/` 에 직접 놓는다.

`docker/pinpoint/hbase/` (`hbase-site.xml`, `hbase-create.hbase`, `entrypoint.sh`) 와 `docker/pinpoint/pinot/init.sh` 는 git 추적 대상이라 별도 다운로드 단계 없음. `pinot-init` 컨테이너만 첫 부팅 시 GitHub 에서 7개 테이블 schema/config 를 가져온다 (raw.github 의존 — 폐쇄망이면 init 스크립트의 curl 단계가 fail-fast).

### 기동 / 종료

```bash
docker compose --profile pinpoint up -d         # 첫 기동 시 HBase schema + Pinot 테이블 등록 자동 — 2~3분
docker compose --profile pinpoint ps            # 13개 서비스 (기존 2 + Pinpoint 11; Pinpoint 11 = core 4 + metric 7) 상태 확인
docker compose --profile pinpoint stop          # 데이터 유지 + 정지
docker compose --profile pinpoint down          # 컨테이너 제거 (볼륨 유지)
docker compose --profile pinpoint down -v       # 컨테이너 + 볼륨 제거 (Pinpoint 데이터 초기화)
```

`--profile pinpoint` 를 빼고 같은 명령을 실행하면 기존 postgres/redis 만 영향을 받는다 — Pinpoint 서비스는 건드리지 않는다.

`pinpoint-kafka-init` 와 `pinot-init` 는 정상 종료 (exit 0) 후 `Exited` 상태로 남는다 — 정상이며 다음 부팅에서 재실행된다 (멱등하게 작성됨).

### app 부착 — `-Ppinpoint=true`

```bash
set -a && source .env && set +a       # 셸에 PINPOINT_* 환경변수를 export
./gradlew :app:bootRun -Ppinpoint=true
```

- `-Ppinpoint=true` 가 빠지면 `bootRun` 은 기존 동작 그대로 — APM 미부착.
- `-Ppinpoint=true` 인데 `PINPOINT_AGENT_PATH` 가 없으면 bootRun task 실행 단계에서 즉시 한국어 에러로 fail-fast (silent skip 방지).
- 다른 환경변수 (`PINPOINT_COLLECTOR_HOST` / `AGENT_ID` / `APPLICATION_NAME`) 는 미설정 시 build script 의 default 값을 쓴다.
- `PINPOINT_AGENT_PATH` 가 상대경로면 build script 가 프로젝트 루트 기준으로 resolve 한다 (`:app:bootRun` 의 CWD 가 `app/` 이지만 `app/build.gradle.kts` 가 `rootDir` 로 다시 푼다). `.env` 의 default `./docker/pinpoint/agent/...` 그대로 사용 가능.
- IntelliJ Run Configuration 에서는 `Gradle Arguments` 에 `-Ppinpoint=true` 추가 + EnvFile 플러그인으로 `.env` 주입 (Postgres 와 동일 방식).

> Pinpoint 의 system property override: `-Dpinpoint.agentId` / `-Dpinpoint.applicationName` 은 agent bootstrap 이 직접 읽고, `-Dprofiler.transport.grpc.collector.ip` 는 agent 의 `pinpoint.config` 키를 동명 시스템 프로퍼티로 덮어쓰는 동작에 의존한다. Pinpoint 3.x agent 가 둘 다 지원한다 — 빌드 스크립트는 같은 `jvmArgs` 한 리스트로 넘긴다.

### 환경변수

| 키 | 필수 여부 | default | 용도 |
|----|----------|---------|------|
| `PINPOINT_VERSION` | 선택 | `3.0.5` | compose 이미지 태그 + `download.sh` 다운로드 버전 + Pinot init 의 git ref (`v${VERSION}`) |
| `PINPOINT_COLLECTOR_HOST` | 선택 | `localhost` | 호스트 agent → collector IP. 로컬 dev 는 `localhost` |
| `PINPOINT_AGENT_PATH` | **필수 (`-Ppinpoint=true` 시)** | 없음 | `-javaagent:` 대상 bootstrap jar 경로. 상대경로면 rootDir 기준 |
| `PINPOINT_AGENT_ID` | 선택 | `crispin-lab-local` | Pinpoint 에서 보는 인스턴스 식별자 (1~24자) |
| `PINPOINT_APPLICATION_NAME` | 선택 | `crispin-lab` | Pinpoint 의 application 그룹 키 |
| `PINPOINT_ADMIN_PASSWORD` | **권장 (`--profile pinpoint` 시)** | `CHANGE_ME` | pinpoint-web admin 계정. host 8079 로 노출되므로 default 그대로 두지 않는다. |
| `PINPOINT_MYSQL_USER` / `_PASSWORD` / `_ROOT_PASSWORD` | 선택 | `pinpoint` | web 의 user/role MySQL 자격증명 (host 노출 없음, 로컬 전용) |

### Web UI

- **`http://localhost:8079`** — pinpoint-web. (포트 8079 는 app 의 default 8080 과 충돌을 피하기 위한 매핑.)
  - 좌측 상단 application 셀렉터에서 `PINPOINT_APPLICATION_NAME` 값을 선택. 첫 요청이 들어와야 application 이 등록되므로 bootRun 직후 endpoint 를 한 번 쳐 본다.
  - **Inspect 탭** — `/api/inspector/*` 가 Pinot 을 query. 데이터가 채워지려면 agent → collector → Kafka → Pinot 경로가 흐를 시간 (1~2분) 필요.
- **`http://localhost:9000`** — Pinot UI. 테이블 목록 / 쿼리 콘솔. Inspector 데이터가 실제로 들어오는지 확인할 때.

### standalone HBase 의 트레이드오프

3.0.5 의 default `pinpoint-hbase` 이미지는 distributed mode + 외부 ZK quorum (zoo1/zoo2/zoo3) 으로 부팅된다. Apple Silicon 의 Rosetta x86_64 에뮬레이션 환경에서는 master JVM 이 60s+ GC pause 를 겪어 ZK 세션이 만료되고 init 가 영원히 hang. 다음 두 가지로 우회한다 — 둘 다 로컬 dev 전용:

1. **`hbase-site.xml` override** — `cluster.distributed=false` 로 standalone (master + regionserver + 임베디드 ZK 한 JVM). cross-process ZK 세션 이슈 자체가 사라진다. collector / web 는 `pinpoint-hbase:2181` 로 직접 연결.
2. **`hbase-create.hbase` override** — default schema 의 `SPLITS` (64-region pre-split) / `NUMREGIONS` 절을 모두 제거. 만들 region 수가 ~900 → 16 으로 급감해 master GC 부담 해소.

대가:
- **production 부하 시뮬레이션 부적합** — pre-split 이 없어 단일 region 에 쓰기 hotspot. 부하 테스트는 별도 환경에서.
- **자동 복구는 entrypoint wrapper 가 책임** — image 의 default CMD (`tail -f /dev/null` 트레일러) 는 HBase JVM 이 죽어도 컨테이너가 살아남게 만든다. `docker/pinpoint/hbase/entrypoint.sh` 가 JVM PID 를 polling 해서 사라지면 컨테이너도 종료시키고, `restart: unless-stopped` 가 자동 복구한다.

### 자주 빠뜨리는 것

- **`--profile pinpoint` 누락** — collector 가 안 떠 있어 agent 가 연결을 못 잡는데 부팅 자체는 성공해 silent 한 미수집 상태가 된다. 부착 직후 web UI 에서 application 노출 여부로 확인.
- **agent jar 다운로드 누락** — `-Ppinpoint=true` 가 fail-fast 한다. 메시지의 안내대로 `download.sh` 실행.
- **mysql-init SQL 다운로드 누락** — `pinpoint-mysql` 컨테이너가 첫 boot 에 init SQL 을 실행하지 못해 pinpoint-web 이 부팅 실패 상태로 hang. `down -v` 로 볼륨을 비운 뒤 `mysql-init/download.sh` 실행 → 다시 `up -d`.
- **Inspect 탭에 차트가 안 그려짐** — 새로 부팅 직후엔 정상. agent 가 inspector-stat 토픽 publish → Pinot ingest 까지 1~2분. 그 뒤에도 비면 `pinot-init` 가 정상 종료했는지 (`docker compose logs pinot-init` 마지막 줄에 `successfully added`), `http://localhost:9000` 의 Tables 메뉴에 7개 (uriStat / tag / double / dataType / exceptionTrace / inspectorStatAgent00 / inspectorStatApp) 가 있는지 확인.
- **HBase 가 한 번 죽었다 살아난 후 region lock** — 과거 패턴. 현재는 entrypoint wrapper 가 죽으면 컨테이너 자체를 내려서 docker restart 가 깨끗이 가져옴. 그래도 region 이 이상하면 `docker volume rm crispin-lab_pinpoint_hbase_data` 로 wipe + 재기동 + app 재기동 (agent 가 application 등록을 다시 보내야 server list 에 노출).
- **app 재기동 없이 collector / web 만 restart** — agent 가 첫 부팅 시 한 번만 application 등록을 보내므로 hbase 가 wipe 되면 server list 가 빈다. 이 경우 app 도 재기동.
- **포트 8079 ↔ 8080 ↔ 9000 혼동** — 8079 = pinpoint-web, 8080 = app, 9000 = pinot-controller UI.
- **`PINPOINT_ADMIN_PASSWORD` 미설정** — pinpoint-web 이 `CHANGE_ME` literal 로 부팅된다. host 8079 가 다른 동료에게 노출되는 환경에서는 반드시 별도 값으로.
- **compose project name 충돌** — `name: crispin-lab` 으로 묶여 있어, 다른 worktree 에서 `--profile pinpoint up` 을 또 돌리면 같은 볼륨을 쓰려 경합. 한 worktree 에서만 띄운다.

## 스코프 외

- **Flyway / SQL 마이그레이션** — `migration.md` 가 책임. 위치(`lab-{domain}/app/src/main/resources/db/migration/`), 네이밍, 테스트 전략(Testcontainers + Flyway) 은 모두 거기 명시.
- **운영 배포 (CI/CD, Helm 등)** — 별도 티켓. Dockerfile 만 사전 정의.
- **Pinpoint sampling 정책 / custom plugin / log 연동 (MDC traceId ↔ pinpoint txid) / SSO** — 본 티켓 범위 외. 도입 시 별도 티켓.
- **telegraf 기반 host metric 수집** — official metric compose 에는 포함이지만 본 프로젝트는 application JVM stat 만 보면 충분해 미포함. 호스트 CPU/disk/network 까지 보려면 telegraf 추가 + system-metric-* 토픽 publish 활성.
- **Pinot 멀티 노드 / replication** — 단일 broker + replicasPerPartition=1 설정. 운영 도입 시 별도 티켓.
