# 패키지 구조

> **이 문서의 범위**: `lab-{domain}` 모듈 안에서 어떤 코드가 어느 패키지에 들어가는지의 표준.
>
> **모듈 레벨 (lab-common / lab-{domain}/domain / lab-{domain}/app / app)**: `architecture.md`
> **각 계층의 코드 형태**: `entity.md`, `usecase-request.md`, `usecase-implementation.md`, `repository.md`, `controller.md`, `test.md`

## 핵심 원칙

**기준은 한 가지: Spring / Exposed / HTTP 어노테이션·import 가 필요한가.**

| 필요 없음 (pure Kotlin) | `lab-{domain}/domain` |
| 필요 있음 (`@Service`, `Table`, `@RestController` 등) | `lab-{domain}/app` |

Port **인터페이스** 는 pure Kotlin 으로 작성 가능하므로 domain 모듈. Port **구현체(UseCase impl, 어댑터)** 는 Spring 또는 Exposed 가 들어오므로 app 모듈.

## 모듈 ↔ 패키지 매핑

`lab-space` 기준 (다른 도메인도 동일 패턴 — `lab-user`, `lab-team` 등은 `space` 자리에 도메인명).

### `lab-space/domain` (pure Kotlin)

```
com.crispinlab.space.domain.{aggregate}                    # Entity, EntityId, Value Object
com.crispinlab.space.application.port.incoming.{aggregate} # 개별 UseCase 인터페이스 (PageGetting, PageEditing)
com.crispinlab.space.application.port.outgoing.{aggregate} # Repository / Search 인터페이스
```

UseCase 베이스 인터페이스(`com.crispinlab.common.application.UseCase`) 는 cross-cutting 위치라 `lab-common` 에 있다 — `architecture.md` 의 모듈 레이아웃 참조.

여기 들어가는 코드는 **Spring / Exposed / Spring Web import 0 개**. 이 룰을 깨면 모듈 경계 자체가 무너진다 (`architecture.md`).

### `lab-space/app` (Spring + Exposed + Spring Web)

```
com.crispinlab.space.application.usecase.{aggregate}       # UseCase 구현체 (@Service)
com.crispinlab.space.adapter.persistence.{aggregate}       # Exposed Table 객체 + Repository 구현체
com.crispinlab.space.adapter.web.{aggregate}               # Controller, Body DTO
com.crispinlab.space.adapter.web.auth                      # Auth (인증 컨텍스트 추출)
com.crispinlab.space.config                                # Spring config, Bean 등록
```

`adapter.{기술}` 형태로, 어댑터 종류가 늘어도 같은 결로 추가 (예: `adapter.search`, `adapter.messaging`).

### `lab-composition/app` (BFF / Composition)

```
com.crispinlab.composition                                     # CompositionModule marker (component scan 루트)
com.crispinlab.composition.adapter.web.{aggregate}             # XxxCompositionController
com.crispinlab.composition.adapter.{domain}                    # 다른 도메인의 outbound port 를 소비하는 어댑터
com.crispinlab.composition.application.usecase.{aggregate}     # BFF UseCase 구현체 (@Service)
com.crispinlab.composition.application.port.incoming.{aggregate}  # BFF 소유 UseCase 인터페이스 (XxxComposition)
com.crispinlab.composition.application.port.outgoing.{domain}  # BFF 소유 outbound port (Lookup 류)
```

- `adapter.{domain}` 의 sub-directory 는 **target 도메인** 이름 (`adapter/user/`, `adapter/space/`) — 기술 스택이 아니다. 도메인 어댑터의 `adapter.{기술}` (persistence / web) 과 다른 축.
- 한 target 도메인 안에 여러 lookup 어댑터가 필요해지면 **파일명** (`UserHandleLookupAdapter.kt`, `UserEmailLookupAdapter.kt`) 으로 구분하고 sub-package (`adapter/user/handle/`) 를 두지 않는다. 어댑터 수가 커져 파일 트리가 얇은 도메인이 붐비면 그때 재편.
- `domain.*` 패키지는 **없다** — BFF 는 자기 entity 를 두지 않고 도메인 aggregate 를 재사용한다. inbound port (UseCase) 는 도메인과 대칭으로 갖는다 (조립 로직·트랜잭션 경계의 자리 — `architecture.md` "BFF/Composition 계층 — 책임 경계" / "트랜잭션 경계" 참조).

## 새 aggregate 추가 체크리스트

`Comment` 를 추가한다고 가정. 만들어야 하는 패키지·파일:

### domain 모듈

- [ ] `domain/comment/Comment.kt` — Entity
- [ ] `domain/comment/CommentId.kt` — EntityId (`value class`)
- [ ] `domain/comment/CommentErrorCode.kt` — `ErrorCode` enum (throw 케이스를 항목화. `error-messages.md` 참조)
- [ ] `application/port/incoming/comment/CommentRegistering.kt` — UseCase 인터페이스
- [ ] `application/port/outgoing/comment/CommentRepository.kt` — Repository 인터페이스

### app 모듈

- [ ] `application/usecase/comment/CommentRegisteringUseCase.kt` — 구현체
- [ ] `adapter/persistence/comment/Comments.kt` — Exposed Table 객체
- [ ] `adapter/persistence/comment/ExposedCommentRepository.kt` — Repository 구현체
- [ ] `adapter/web/comment/CommentRegisteringController.kt` — Controller

### 테스트

- [ ] `lab-space/domain/src/testFixtures/.../testsupport/Fixtures.kt` 에 `basicComment(...)` 추가 — entity·VO 생성 헬퍼는 domain testFixtures.
- [ ] `lab-space/app/src/test/.../application/usecase/comment/CommentRegisteringUseCaseTest.kt`
- [ ] `lab-space/app/src/test/.../adapter/web/comment/CommentRegisteringControllerTest.kt`
- [ ] `lab-space/app/src/test/.../adapter/persistence/comment/ExposedCommentRepositoryTest.kt`

### Bean 등록

- [ ] `config/Beans.kt` (또는 동일 책임의 `@Configuration`) 에 UseCase 구현체를 빈으로 등록 (`@Service` 어노테이션이 있다면 component scan 으로 충분).

## 새 크로스도메인 조립 endpoint 를 BFF 로 추가 체크리스트

여러 도메인의 데이터가 응답에 조립되는 endpoint 를 새로 만들거나, 기존 도메인 controller 를 BFF 로 이관할 때. `Page` 조회 endpoint 를 BFF 로 옮긴다고 가정.

### lab-composition/app

- [ ] `application/port/incoming/{aggregate}/XxxComposition.kt` — BFF UseCase 인터페이스 (`UseCase<Request, Result>`). Request 는 controller 로부터 raw 파라미터 pass-through, Result 는 응답 payload shape.
- [ ] `application/usecase/{aggregate}/XxxCompositionUseCase.kt` — `@Service` + `TransactionProvider` 주입. read composition 은 `perform` 진입에서 `transactional(readOnly = true) { }` 로 감쌈, write composition 은 outer tx 없이 lookup 만 별도 `transactional(readOnly = true) { }` (`architecture.md` "트랜잭션 경계"). 도메인 UseCase + BFF lookup 조립.
- [ ] `adapter/web/{aggregate}/XxxCompositionController.kt` — `XxxComposition` 만 주입. request/response 매핑만.
- [ ] BFF outbound port 가 없으면 `application/port/outgoing/{domain}/XxxLookup.kt` 신설. batch 시그니처 우선 (`handlesOf(ids): Map<UserId, String>`).
- [ ] 어댑터가 없으면 `adapter/{domain}/XxxLookupAdapter.kt` 신설. `@Component` + 도메인의 outbound port (`UserHandleQuery`, `SpaceMemberRepository`) 를 주입해 소비.
- [ ] UseCase 단위 테스트 `application/usecase/{aggregate}/XxxCompositionUseCaseTest.kt` — 조립 로직 검증 + `RecordingTransactionProvider` 로 "lookup 이 tx 블록 안에서 호출" 검증 (read: perform 전체 readOnly wrap / write: 도메인 perform 은 tx 밖 + lookup 만 readOnly tx — LAB-156 회귀 방지) + 입력 형식 오류 → `IllegalArgumentException` 전파 케이스.
- [ ] controller 테스트는 `CompositionAppControllerDescribeSpec` 상속. 조립 관련 검증은 UseCase 테스트로 이동하고 controller 테스트는 라우팅·직렬화·400/401·document 산출만.

### 도메인 module 축소

- [ ] 도메인 UseCase Result 에서 크로스도메인 파생 스칼라 (`authorHandle` 등) 제거 → identifier (`authorId: UserId`) 만 남긴다.
- [ ] 도메인 UseCase 구현에서 다른 도메인의 outbound port 주입 제거 (`UserHandleQuery` 등). 도메인은 handle 조회 무지 상태로.
- [ ] 도메인 module 이관되는 endpoint 의 기존 controller 제거 (`lab-{domain}/app/adapter/web/{aggregate}/XxxController.kt`). 응답 URL / OpenAPI schema name 은 그대로 유지 (BFF 가 같은 URL 로 응답, 클라이언트 계약 불변).

### N+1 방지

- [ ] 리스트 endpoint 는 항상 batch lookup 한 번 — `lookup.handlesOf(items.map { it.authorId }.toSet())` 로 모아서 Map 을 받은 뒤 items 매핑에 재사용. 개별 lookup 반복 금지.
- [ ] 단건 endpoint 는 `lookup.handleOf(id)` extension helper 한 줄 (`controller.md` "Payload / 조립 패턴" 참조).

### 빌드 인프라

- [ ] BFF 가 새 도메인 module 을 소비해야 하면 `lab-composition/app/build.gradle.kts` 에 `implementation(projects.labXxx.domain)` + `implementation(projects.labXxx.app)` 두 줄 추가. 기존 소비 도메인이면 추가 작업 없음.
- [ ] **새 도메인 module 소비를 새로 추가한 경우엔 `app/Dockerfile` 의 builder 단계 COPY 두 줄** (`lab-<domain>/domain/build.gradle.kts` + `lab-<domain>/app/build.gradle.kts`) **도 함께 추가** — dependency cache 유지 (본 문서 "다른 도메인 모듈 추가 시" 절 정합).
- [ ] 기존 소비 도메인의 endpoint 추가는 build 파일 변경 없이 파일 추가만이라 Docker 캐시 갱신 불필요.

## 결정 근거

### Q1. UseCase 인터페이스는 왜 domain 모듈인가?

UseCase 는 **비즈니스 의도** 를 표현하는 계약이다. `interface PageEditing` 자체는 pure Kotlin 으로 표현 가능하고, Spring 의존이 없다. 따라서 가장 안쪽 모듈(`domain`)에 두어, app 모듈이 갈아 끼워질 가능성을 열어둔다 (예: 다른 framework 어댑터 실험).

### Q2. UseCase 구현체는 왜 app 모듈인가?

대개 `@Service`, `@Transactional`, 외부 어댑터 주입이 필요하다. 이 의존이 들어오는 순간 domain 모듈의 "pure Kotlin" 규약이 깨진다.

### Q3. testsupport 는 어느 모듈?

**책임으로 가른다.**

- **`lab-space/domain/src/testFixtures/kotlin/com/crispinlab/space/testsupport`** — entity·VO 생성기 (`Fixtures.basicSpace`, `Dummies.DUMMY_INSTANT` 등). domain 모듈의 entity 테스트와 app 모듈의 UseCase·Controller·Repository 테스트가 모두 같은 fixture 를 공유해 default 값이 어긋나지 않도록. `java-test-fixtures` 플러그인이 적용되어 있고, `lab-space/app/build.gradle.kts` 가 `testImplementation(testFixtures(projects.labSpace.domain))` 로 받는다.
- **`lab-space/app/src/test/kotlin/com/crispinlab/space/testsupport`** — UseCase 단위 테스트 도구 (`DummyTransactionProvider` 등). app 모듈에서만 의미가 있는 헬퍼는 여기.

새 도메인을 추가할 때도 같은 분할 — entity Fixture 는 domain testFixtures, UseCase 도구는 app testsupport.

### Q4. `application` 과 `adapter` 가 같은 패키지 prefix 를 갖는 이유?

같은 도메인의 application 계층(`application.usecase`)과 adapter 계층(`adapter.web`)이 공통 prefix(`com.crispinlab.space`)를 공유해, 도메인 단위 검색·grep 이 한 번에 가능. 모듈은 분리되어도 도메인 식별은 같다.

## 자주 헷갈리는 것

- **UseCase 인터페이스를 app 모듈에 둠** — 의존이 거꾸로 흐름. 항상 domain.
- **port 인터페이스에 Spring 어노테이션 (`@Repository` 등) 부착** — port 는 pure Kotlin. `@Repository` 는 어댑터 구현체에만.
- **Entity 안에 `@Entity`, `@Table`, `@Column` (JPA) 어노테이션** — Exposed 기반이므로 JPA 어노테이션은 애초에 등장할 일 없지만, 혹시 인터페이스 마이그레이션 등으로 끌려 들어오면 entity 가 domain 모듈을 벗어나야 함 → 모듈 경계 위반. domain entity 는 어떤 ORM 어노테이션도 없어야 한다.
- **컨트롤러 `Body` DTO 를 `application.port.incoming` 에 넣음** — Body 는 HTTP 외부 계약이라 `adapter.web` 에 controller 와 같이. UseCase Request 와 분리.
- **`adapter.persistence` 안에서 도메인 메서드 호출** — `entity.publish()` 같은 도메인 동작은 UseCase 책임. 어댑터는 매핑·SQL 만.
- **새 `aggregate` 만들면서 `domain.{aggregate}` 만 만들고 port 패키지를 빠뜨림** — 일관성을 위해 7 개 패키지를 한 PR 로 묶는 편이 추적이 쉽다 (사용 안 하는 빈 패키지는 두지 말 것 — 첫 인터페이스/파일과 같이 생성).

## 다른 도메인 모듈 추가 시

`lab-user` 같은 도메인 모듈을 추가할 때 — 본 문서를 그대로 적용. 패키지명만 `space` → `user` 로 바꾼다.

```
lab-user/domain   → com.crispinlab.user.domain.*
                    com.crispinlab.user.application.port.*
lab-user/app      → com.crispinlab.user.application.usecase.*
                    com.crispinlab.user.adapter.*
                    com.crispinlab.user.UserModule        # component scan 마커
```

도메인 간 호출이 필요하면 — port 를 통해서만. 한 도메인의 entity 를 다른 도메인이 직접 import 하지 않는다.

새 도메인 모듈은 `app` 모듈의 `@SpringBootApplication(scanBasePackageClasses = [...])` 에 marker 클래스 한 줄 추가가 필요 (`architecture.md` "component scan 범위" 참조).

또한 `app/Dockerfile` 의 builder 단계가 의존성 캐시를 위해 서브모듈 `build.gradle.kts` 경로를 하드코딩한다 — 새 도메인 모듈을 추가하면 `COPY lab-<domain>/domain/build.gradle.kts ...` / `COPY lab-<domain>/app/build.gradle.kts ...` 두 줄을 같이 추가한다. 누락 시 docker 이미지 빌드는 풀-소스 복사 뒤 단계에서 결국 성공하지만 캐시 효율이 깨진다 (`dev-infra.md` 참조).

## `app` 모듈 패키지 구조

```
com.crispinlab.app                # @SpringBootApplication 진입 클래스
com.crispinlab.app.config         # cross-cutting 설정 (필요 시)
```

- 진입 클래스 패키지는 `com.crispinlab.app` — default scan 이 `com.crispinlab` 루트로 퍼지지 않게.
- 도메인 어댑터·UseCase 는 `lab-<domain>/app` 에 둔다. `app` 모듈 안에 도메인 코드를 두지 않는다.
