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
com.crispinlab.space.application.port.incoming             # UseCase 베이스 인터페이스
com.crispinlab.space.application.port.incoming.{aggregate} # 개별 UseCase 인터페이스 (PageGetting, PageEditing)
com.crispinlab.space.application.port.outgoing.{aggregate} # Repository / Search 인터페이스
```

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

## 새 aggregate 추가 체크리스트

`Comment` 를 추가한다고 가정. 만들어야 하는 7 개 패키지·파일:

### domain 모듈

- [ ] `domain/comment/Comment.kt` — Entity
- [ ] `domain/comment/CommentId.kt` — EntityId (`value class`)
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

## `app` 모듈 패키지 구조

```
com.crispinlab.app                # @SpringBootApplication 진입 클래스
com.crispinlab.app.config         # cross-cutting 설정 (필요 시)
```

- 진입 클래스 패키지는 `com.crispinlab.app` — default scan 이 `com.crispinlab` 루트로 퍼지지 않게.
- 도메인 어댑터·UseCase 는 `lab-<domain>/app` 에 둔다. `app` 모듈 안에 도메인 코드를 두지 않는다.
