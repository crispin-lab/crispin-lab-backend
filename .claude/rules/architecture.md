# 아키텍처

## 모듈 레이아웃
- `lab-common` — cross-cutting 전용 (snowflake ID, 예외, pagination, 트랜잭션 인터페이스 등). **도메인 로직·Spring 의존은 두지 않는다.**
- `lab-common-infra` — `lab-common` 인터페이스의 Spring/인프라 어댑터 모음 (예: `TransactionProvider` 구현). Spring Boot auto-config 으로 노출.
- `lab-space/domain` — 순수 도메인. **Spring / Exposed / HTTP import 금지.**
- `lab-space/app` — Spring + Exposed 어댑터: controller, repository 구현, search 어댑터.
- `app` — `@SpringBootApplication`이 있는 실행 가능 모듈. 이 모듈만 `bootJar`를 활성화한다.

향후 도메인도 같은 패턴: `lab-<domain>/domain` + `lab-<domain>/app`.

## 컨벤션 플러그인
`build-logic/convention/` 안의 `crispinlab.*` 네임스페이스에 위치.

사용자가 직접 적용:
- `crispinlab.jvm` — Kotlin JVM 베이스 (kotlinter, kotest, JUnit Platform, git hooks)
- `crispinlab.kopring.service` — Spring Boot 라이브러리 (`spring-boot-starter` 포함)
- `crispinlab.kopring.web` — Spring Boot 라이브러리 (`spring-boot-starter-web` + validation)
- `crispinlab.kopring.exposed` — Exposed Spring Boot starter + h2
- `crispinlab.snowflake`, `crispinlab.kotlin.serialization`, `crispinlab.rest-assured` — 선택적 mix-in

내부 building block (다른 컨벤션이 자동 적용):
- `crispinlab.kotest`, `crispinlab.kopring.base`, `crispinlab.kopring.test`

## 빌드 모드
kopring 모듈은 `crispinlab.kopring.base`를 통해 **라이브러리 모드** (`bootJar` off, `jar` on)가 기본이다. 실행 가능한 `app` 모듈만 `bootJar`를 다시 켜고 `jar`를 끈다.

Spring Boot 4.0.3 BOM은 `crispinlab.kopring.base` 안에서 `platform()`으로 적용한다. `io.spring.dependency-management` 플러그인은 **사용하지 않는다.**
