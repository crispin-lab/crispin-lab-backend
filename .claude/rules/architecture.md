# 아키텍처

## 모듈 레이아웃
- `lab-common-domain` — 도메인 마커 (`LongValue`, `EntityId`, `Entity<ID>`). **pure Kotlin. 다른 모듈 의존 없음.** 도메인 모듈 (`lab-<domain>/domain`) 이 `api(...)` 로 노출해 consumer 가 super-type 을 볼 수 있게 한다.
- `lab-common-port` — application/inbound port 시그니처 super type (`UseCase<Req, Res>`, `PageRequest`, `PageResult`, `ErrorCode`). **pure Kotlin.** 도메인 port 인터페이스가 이 모듈의 타입을 시그니처에 노출하므로 도메인 모듈이 `api(...)` 로 가져간다.
- `lab-common` — cross-cutting 인프라성 인터페이스 (`IdGenerator`, `Clock`, `TransactionProvider`, `LogContext`) + 도메인 예외 클래스 (`DomainException`, `NotFoundException`, `ConflictException`). **pure Kotlin, Spring 의존 두지 않는다.** `api(labCommonPort)` 로 `ErrorCode` 노출. 도메인 모듈은 의존 안 함 (어댑터·UseCase 구현체만 `implementation(...)` 로 사용).
- `lab-common-infra` — `lab-common` 인터페이스의 Spring/인프라 어댑터 모음 (예: `TransactionProvider` 구현, `EntityIdJacksonConfiguration`). Spring Boot auto-config 으로 노출 — `META-INF/spring/...AutoConfiguration.imports` 등록 + 클래스에 `@AutoConfiguration` (일반 `@Configuration` 금지). 두 표현이 어긋나면 ordering 메타데이터(`@AutoConfigureAfter` / `afterName`) 가 누락된다.
- `lab-api-support` — 컨트롤러 테스트 공용 도구 (`ControllerDescribeSpec` + `FieldBuilder` DSL, restdocs-api-spec wrapper). 다른 도메인 `app` 모듈이 `testImplementation(projects.labApiSupport)` 로 받는다. main source 가 테스트 도구라서 외부 `kotest`, `mockk`, `spring-restdocs`, `restdocs-api-spec` 의존을 `api(...)` 로 노출한다.
- `lab-space/domain` — 순수 도메인. **Spring / Exposed / HTTP import 금지.** `api(labCommonDomain) + api(labCommonPort)` 만 — 인프라성 코드 (`TransactionProvider`, `LogContext` 등) 가 consumer 에게 transitive 노출되지 않게.
- `lab-space/app` — Spring + Exposed 어댑터: controller, repository 구현, search 어댑터. `ExposedEntityRepository<E, I>` base 가 어댑터 보일러플레이트를 통합 (`repository.md` 참조).
- `app` — `@SpringBootApplication`이 있는 실행 가능 모듈. 이 모듈만 `bootJar`를 활성화한다. 진입 클래스는 `com.crispinlab.app.Application` (루트 `com.crispinlab` 에 두면 default scan 이 `lab-common-infra` 의 auto-config 패스와 같은 패키지를 이중으로 훑게 된다).

향후 도메인도 같은 패턴: `lab-<domain>/domain` + `lab-<domain>/app`.

## lab-common 분리의 의도

`lab-common` 을 세 모듈 (`lab-common-domain` / `lab-common-port` / `lab-common`) 로 나눈 이유: **모듈 경계를 build 설정으로 강제한다.** 도메인 모듈 (`lab-<domain>/domain`) 이 `api(labCommon)` 으로 통째 노출하면 인프라성 코드까지 consumer 에게 transitive 로 흘러간다 — 도메인 entity 가 `TransactionProvider` 같은 어댑터 인터페이스를 잘못 import 해도 컴파일러가 못 막는다. 분리하면 super-type 가시성이 정말 필요한 마커/포트만 `api(...)` 로 노출되고, 인프라성 인터페이스는 `lab-{domain}/app` 등의 어댑터 모듈에서만 `implementation(...)` 으로 받는다. 새 도메인 모듈 추가 시 build.gradle.kts 한 줄 (`api(labCommonDomain) + api(labCommonPort)`) 만으로 의도 정합.

### 패키지 정책 — `com.crispinlab.common.exception` 의 split package

`ErrorCode` 는 도메인 모듈이 `PageErrorCode : ErrorCode` 형태로 implement 하는 super-type 이라 `lab-common-port` 로 옮겼지만, 패키지 `com.crispinlab.common.exception` 은 그대로 유지한다. `NotFoundException` / `ConflictException` / `DomainException` 은 `lab-common` 의 같은 패키지에 남았다 — 모듈은 다르지만 패키지가 같은 **split package** 상태. JVM 동작에는 문제 없고, import 경로 변경을 도메인 모듈 전반에 일으키지 않으려는 의도. IDE 의 "이 클래스가 어느 모듈?" 추적이 약간 추가 비용이 들지만, port super-type 인 `ErrorCode` 와 예외 구현체가 같은 패키지에 모여 있는 도메인적 일관성이 더 크다.

### 의존 정책 — `lab-common-infra` 가 `labCommonDomain` 을 `implementation` 으로 받는 이유

`EntityIdSerializer` 의 시그니처에 `EntityId` 가 등장하지만 `lab-common-infra` 는 이 의존을 `api` 가 아닌 `implementation` 으로만 노출한다. consumer 가 `EntityIdSerializer` 를 직접 import 할 일은 거의 없고 (Spring Boot 의 Jackson auto-config 가 `SimpleModule` 빈을 자동 wiring), 직접 참조하는 곳 (예: `lab-api-support` 의 `ControllerDescribeSpec`) 은 이미 별도로 `api(labCommonDomain)` 을 명시한다. `lab-common-infra` 가 `api(labCommonDomain)` 으로 올리면 transitive 노출이 늘어나면서 분리 의도가 약해진다 — `EntityId` 가 필요한 consumer 는 자기 build.gradle.kts 에서 명시 의존을 갖는 것이 정합.

## component scan 범위

`@SpringBootApplication(scanBasePackageClasses = [Application::class, SpaceModule::class, ...])` 로 type-safe 하게 명시한다. 도메인 모듈마다 `com.crispinlab.<domain>` 루트에 marker 인터페이스(`SpaceModule`, 향후 `UserModule` 등) 한 개를 두고, 진입 클래스의 `scanBasePackageClasses` 에 추가한다.

- **문자열 `scanBasePackages` 금지** — 패키지 리네임·이동을 컴파일러가 못 잡는다. marker 클래스 참조로 통일.
- **default scan (좁힘 안 함) 금지** — `com.crispinlab.common.*` 까지 스캔되어 auto-config 노출 모듈과 충돌 가능.
- 새 도메인 모듈을 추가하면 marker 클래스 신설 + `scanBasePackageClasses` 에 한 줄 추가가 PR 체크리스트.
- **marker 1 모듈 1 개** — 같은 도메인에 marker 를 둘 이상 만들지 않는다. 한 marker 의 패키지(`com.crispinlab.<domain>`) 가 그 도메인의 component scan 루트라, 같은 패키지에 marker 가 2 개여도 scan 결과는 같지만 의미 분산. 그리고 다른 도메인의 marker 를 잘못 import 해 `scanBasePackageClasses` 에 두 번 들어가지 않게 주의 — 같은 marker 가 중복 등록되면 Spring 이 한 번만 처리하지만 의도가 흐려진다.

## auto-config ordering

`lab-common-infra` 의 `@AutoConfiguration` 이 다른 auto-config 빈에 의존할 때 (예: `@ConditionalOnBean(PlatformTransactionManager)`), ordering 을 `afterName = ["..."]` 로 명시한다. 외부 starter 가 메이저 업그레이드를 거치면서 클래스 패키지가 이동하면 (예: `exposed-spring-boot-starter 1.x` 가 `org.jetbrains.exposed.v1.*` 로 옮긴 사례) 기존 `@AutoConfigureAfter` 메타데이터가 깨질 수 있어, `afterName` 으로 FQCN 을 직접 강제한다. Spring Boot 버전과 무관하게 외부 의존성 패키지 변경에 안전.

## 컨벤션 플러그인
`build-logic/convention/` 안의 `crispinlab.*` 네임스페이스에 위치.

사용자가 직접 적용:
- `crispinlab.jvm` — Kotlin JVM 베이스 (kotlinter, kotest, JUnit Platform, git hooks)
- `crispinlab.kopring.service` — Spring Boot 라이브러리 (`spring-boot-starter` 포함)
- `crispinlab.kopring.web` — Spring Boot 라이브러리 (`spring-boot-starter-web` + validation)
- `crispinlab.kopring.library` — Spring 환경에서 사용되는 일반 라이브러리 (Spring Boot BOM 을 `api` configuration 으로 노출). `spring-boot-starter-*` 가 필요 없고, 소비 모듈에 BOM-managed 의존을 `api` 로 전파해야 하는 테스트 도구·헬퍼 라이브러리에 적용. 예: `lab-api-support`.
- `crispinlab.kopring.exposed` — Exposed Spring Boot starter + h2
- `crispinlab.snowflake`, `crispinlab.kotlin.serialization`, `crispinlab.rest-assured`, `crispinlab.restdocs` — 선택적 mix-in

내부 building block (다른 컨벤션이 자동 적용):
- `crispinlab.kotest`, `crispinlab.kopring.base`, `crispinlab.kopring.test`

## 빌드 모드
kopring 모듈은 `crispinlab.kopring.base`를 통해 **라이브러리 모드** (`bootJar` off, `jar` on)가 기본이다. 실행 가능한 `app` 모듈만 `bootJar`를 다시 켜고 `jar`를 끈다.

Spring Boot BOM은 `crispinlab.kopring.base` 안에서 `applySpringBootBom()` (`platform()`) 으로 적용한다. 버전은 `gradle/libs.versions.toml` 의 `spring-boot` 키로만 관리. `io.spring.dependency-management` 플러그인은 **사용하지 않는다.**
