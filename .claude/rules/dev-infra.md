# 로컬 dev 인프라

> **범위**: 로컬에서 app 모듈을 띄울 때 사용하는 외부 의존성 (Postgres, Redis) 의 Docker Compose 셋업과 환경변수 규약. Flyway / APM / CI/CD 는 본 문서 범위 외 (별도 티켓).

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

## 스코프 외

- **Flyway / SQL 마이그레이션** — `migration.md` 가 책임. 위치(`lab-{domain}/app/src/main/resources/db/migration/`), 네이밍, 테스트 전략(Testcontainers + Flyway) 은 모두 거기 명시.
- **Pinpoint APM** — 별도 티켓.
- **운영 배포 (CI/CD, Helm 등)** — 별도 티켓. Dockerfile 만 사전 정의.
