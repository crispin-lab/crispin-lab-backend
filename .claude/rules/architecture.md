# 아키텍처

## 모듈 레이아웃
- `lab-common` — cross-cutting 전용 (snowflake ID, 예외, pagination, 트랜잭션 인터페이스 등). **도메인 로직·Spring 의존은 두지 않는다.**
- `lab-common-infra` — `lab-common` 인터페이스의 Spring/인프라 어댑터 모음 (예: `TransactionProvider` 구현). Spring Boot auto-config 으로 노출 — `META-INF/spring/...AutoConfiguration.imports` 등록 + 클래스에 `@AutoConfiguration` (일반 `@Configuration` 금지). 두 표현이 어긋나면 ordering 메타데이터(`@AutoConfigureAfter` 등)가 누락된다.
- `lab-space/domain` — 순수 도메인. **Spring / Exposed / HTTP import 금지.**
- `lab-space/app` — Spring + Exposed 어댑터: controller, repository 구현, search 어댑터.
- `app` — `@SpringBootApplication`이 있는 실행 가능 모듈. 이 모듈만 `bootJar`를 활성화한다. 진입 클래스는 `com.crispinlab.app.Application` (루트 `com.crispinlab` 에 두면 default scan 이 `lab-common-infra` 의 auto-config 패스와 같은 패키지를 이중으로 훑게 된다).

향후 도메인도 같은 패턴: `lab-<domain>/domain` + `lab-<domain>/app`.

## component scan 범위

`@SpringBootApplication(scanBasePackageClasses = [Application::class, SpaceModule::class, ...])` 로 type-safe 하게 명시한다. 도메인 모듈마다 `com.crispinlab.<domain>` 루트에 marker 인터페이스(`SpaceModule`, 향후 `UserModule` 등) 한 개를 두고, 진입 클래스의 `scanBasePackageClasses` 에 추가한다.

- **문자열 `scanBasePackages` 금지** — 패키지 리네임·이동을 컴파일러가 못 잡는다. marker 클래스 참조로 통일.
- **default scan (좁힘 안 함) 금지** — `com.crispinlab.common.*` 까지 스캔되어 auto-config 노출 모듈과 충돌 가능.
- 새 도메인 모듈을 추가하면 marker 클래스 신설 + `scanBasePackageClasses` 에 한 줄 추가가 PR 체크리스트.
- **marker 1 모듈 1 개** — 같은 도메인에 marker 를 둘 이상 만들지 않는다. 한 marker 의 패키지(`com.crispinlab.<domain>`) 가 그 도메인의 component scan 루트라, 같은 패키지에 marker 가 2 개여도 scan 결과는 같지만 의미 분산. 그리고 다른 도메인의 marker 를 잘못 import 해 `scanBasePackageClasses` 에 두 번 들어가지 않게 주의 — 같은 marker 가 중복 등록되면 Spring 이 한 번만 처리하지만 의도가 흐려진다.

## auto-config ordering

`lab-common-infra` 의 `@AutoConfiguration` 이 다른 auto-config 빈에 의존할 때 (예: `@ConditionalOnBean(PlatformTransactionManager)`), ordering 을 `afterName = ["..."]` 로 명시한다. 외부 starter 의 `@AutoConfigureAfter` 메타가 깨져 있어도(예: 클래스 패키지 이동) 우리 ordering 은 보장된다.

## 컨벤션 플러그인
`build-logic/convention/` 안의 `crispinlab.*` 네임스페이스에 위치.

사용자가 직접 적용:
- `crispinlab.jvm` — Kotlin JVM 베이스 (kotlinter, kotest, JUnit Platform, git hooks)
- `crispinlab.kopring.service` — Spring Boot 라이브러리 (`spring-boot-starter` 포함)
- `crispinlab.kopring.web` — Spring Boot 라이브러리 (`spring-boot-starter-web` + validation)
- `crispinlab.kopring.exposed` — Exposed Spring Boot starter + h2
- `crispinlab.snowflake`, `crispinlab.kotlin.serialization`, `crispinlab.rest-assured`, `crispinlab.restdocs` — 선택적 mix-in

내부 building block (다른 컨벤션이 자동 적용):
- `crispinlab.kotest`, `crispinlab.kopring.base`, `crispinlab.kopring.test`

## 빌드 모드
kopring 모듈은 `crispinlab.kopring.base`를 통해 **라이브러리 모드** (`bootJar` off, `jar` on)가 기본이다. 실행 가능한 `app` 모듈만 `bootJar`를 다시 켜고 `jar`를 끈다.

Spring Boot BOM은 `crispinlab.kopring.base` 안에서 `applySpringBootBom()` (`platform()`) 으로 적용한다. 버전은 `gradle/libs.versions.toml` 의 `spring-boot` 키로만 관리. `io.spring.dependency-management` 플러그인은 **사용하지 않는다.**
