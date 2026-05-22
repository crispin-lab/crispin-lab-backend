# Controller 패턴

> **이 문서의 범위**: HTTP 경계의 controller 가 어떤 책임만 가지고 어떻게 UseCase 를 호출하는지.
>
> **UseCase Request/Result**: `usecase-request.md`
> **UseCase 구현**: `usecase-implementation.md`
> **모듈 경계**: `architecture.md`

## controller 의 책임

| 가진다 | 가지지 않는다 |
|--------|---------------|
| HTTP 라우팅 (path, method, status) | 비즈니스 로직 |
| 인증 컨텍스트 추출 (`auth.userId`) | 권한·상태 검증 (UseCase 가 책임) |
| Body / PathVariable → Request 변환 | DB 조회, 엔티티 가공 |
| Result 를 그대로 반환 | Result 를 다시 wrapping (예: `ApiResponse(...)`) |

controller 가 **얇게** 유지되어야 UseCase 가 진짜 비즈니스 단위로 남는다.

## REST 매핑

| 동작 | 메서드 | 경로 |
|------|--------|------|
| 생성 | POST | `/v1/{entities}` |
| 단건 조회 | GET | `/v1/{entities}/{id}` |
| 목록·검색 | GET | `/v1/{entities}?query=&page=&size=` |
| 수정 | PUT | `/v1/{entities}/{id}` |
| 삭제 | DELETE | `/v1/{entities}/{id}` |
| 특정 필드 조회 | GET | `/v1/{entities}/{id}/{field}` |
| 특정 행위 | POST | `/v1/{entities}/{id}/{action}` |

- path 는 영문 복수형, kebab-case 가 아닌 단순 명사.
- 버전 prefix(`/v1`) 는 모든 endpoint 에 동일하게.

## 조회 controller

```kotlin
package com.crispinlab.space.adapter.web.page

import com.crispinlab.space.application.port.incoming.page.PageGetting
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.incoming.page.PageGetting.Result
import com.crispinlab.space.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}")
class PageGettingController(
    private val useCase: PageGetting,
) {
    @GetMapping
    fun get(
        @PathVariable pageId: String,
        auth: Auth,
    ): Result =
        Request(
            pageId = pageId,
            currentUserId = auth.userId,
        ).let { useCase.perform(it) }
}
```

- controller 이름은 UseCase 이름 + `Controller`.
- 메서드 시그니처는 한 식으로 — `Request(...).let { useCase.perform(it) }`.
- Result 는 그대로 반환. 별도 envelope 으로 감싸지 않는다.

## 생성 controller

```kotlin
@RestController
@RequestMapping("/v1/pages")
class PageRegisteringController(
    private val useCase: PageRegistering,
) {
    @PostMapping
    fun register(
        @RequestBody body: Body,
        auth: Auth,
    ) {
        body.toRequestWith(auth.userId)
            .let { useCase.perform(it) }
    }

    data class Body(
        val title: String,
        val body: String,
        val visibility: String,
        val parentPageId: String? = null,
    ) {
        fun toRequestWith(userId: String): Request =
            Request(
                title = title,
                body = body,
                visibility = visibility,
                parentPageId = parentPageId,
                currentUserId = userId,
            )
    }
}
```

- `Body` 는 controller 내부 클래스 — 외부 계약 전용 DTO.
- `Body.toRequestWith(userId)` 패턴 (`conventions.md` "`With` 전치사 규칙": Request 같은 별도 DTO 를 넘길 때 `With`).
- 반환 타입은 `Unit`. UseCase 가 `Unit` 을 반환하는 것과 일치.

## 수정 controller

```kotlin
@RestController
@RequestMapping("/v1/pages/{pageId}")
class PageEditingController(
    private val useCase: PageEditing,
) {
    @PutMapping
    fun edit(
        @PathVariable pageId: String,
        @RequestBody body: Body,
        auth: Auth,
    ): Result =
        body.toRequestWith(pageId = pageId, userId = auth.userId)
            .let { useCase.perform(it) }

    data class Body(
        val title: String,
        val body: String,
        val visibility: String,
    ) {
        fun toRequestWith(pageId: String, userId: String): Request =
            Request(
                pageId = pageId,
                title = title,
                body = body,
                visibility = visibility,
                currentUserId = userId,
            )
    }
}
```

## Auth 인증 컨텍스트 추출

`lab-user/app/.../adapter/web/auth/AuthArgumentResolver` 가 `Authorization: Bearer {token}` 헤더를 받아 `Auth(userId, role)` 로 변환한다. user 도메인이 auth 의 owner 이므로 resolver / `Auth` 타입 모두 lab-user/app 에 위치한다 (`architecture.md` "lab-user/app — cross-cutting auth provider" 참조). 흐름은 어댑터 경계에서 끝난다:

1. **헤더 존재 + Bearer prefix 확인** — 누락/형식 오류 → `AuthenticationException(SessionErrorCode.INVALID_SESSION)` → 401.
2. **토큰 형식 검증** — `SessionToken(raw)` 의 init 가 `sess_<43-base64>` 규약을 검증. 실패 시 401.
3. **세션 lookup** — `SessionService.find(token)` 가 `null` → 401. 같은 호출이 sliding EXPIRE 를 갱신한다 (`RedisSessionService` 내부).
4. **사용자 lookup** — `UserRepository.findBy(userId)` 가 `null` (계정 삭제 등) → 401.
5. **Auth 조립** — `Auth(userId = user.id, role = user.role)`. `Auth.isAdmin()` 확장으로 ADMIN 여부 조회.

검증을 UseCase Request 의 `asUserId()` 변환에 미루지 않는 이유: 인증 게이트키퍼와 도메인 변환의 책임을 섞으면 응답 코드·메시지의 의도가 흐려진다. `AuthArgumentResolver` 에서 한 번에 닫는다.

> 모든 실패 경로가 같은 `INVALID_SESSION` 으로 떨어지는 것은 의도 — 헤더 누락/형식 오류/세션 미존재/사용자 미존재를 응답으로 구분하면 enumeration 정보 누출 (`error-messages.md` "정보 노출 방지" 정합).

### 옵셔널 인증 endpoint — `auth: Auth?`

비로그인 사용자도 PUBLIC 리소스를 볼 수 있어야 하는 GET endpoint 는 controller signature 를 `auth: Auth?` 로 받는다. resolver 가 `parameter.isOptional` (Kotlin nullable) 을 보고 분기:

- **헤더 자체가 없음** → `null` 반환 (anonymous 흐름)
- **헤더가 있는데 invalid** (만료/위변조/세션 miss/사용자 미존재) → `AuthenticationException` (401) 그대로 throw

invalid 토큰을 silently null 로 떨어뜨리지 않는 이유: 클라이언트가 세션 만료를 인지 못 해 자동 재로그인 트리거가 안 걸린다. nullable 타입 자체가 시그널이라 별도 어노테이션 신설 안 함.

### `Auth → 도메인 자체 access type` 변환

다른 도메인의 controller (예: `lab-space/app`) 가 권한을 다룰 때 `Auth` 를 UseCase Request 에 그대로 넘기지 않는다. 도메인이 자기 access control 모델 (예: `Viewer` sealed type) 을 두고, controller 가 변환한다.

```kotlin
// lab-space/app/.../adapter/web/auth/AuthViewerMapping.kt
fun Auth.toMember(): Viewer.Member =
    Viewer.Member(userId = userId, isAdmin = isAdmin)

fun Auth?.toViewer(): Viewer =
    this?.toMember() ?: Viewer.Anonymous
```

- **인증 필수 endpoint** (`auth: Auth`): `auth.toMember()` — `Viewer.Member` (non-null) 로 변환, Request 는 `viewer: Viewer.Member` 로 받는다. sealed variant 직접 시그니처 노출로 호출 측이 Anonymous 케이스를 못 만든다.
- **인증 옵셔널 endpoint** (`auth: Auth?`): `auth.toViewer()` — `Viewer` (sealed parent) 로 변환, Request 는 `viewer: Viewer` 로 받는다.

이름 분리 (`toMember` 가 member, `toViewer` 가 sealed parent) 로 receiver 가 nullable / non-null 인지가 호출부에서 명확. `Auth.toViewer()` / `Auth?.toViewer()` 같은 member/extension 동명 공존을 피한다.

도메인 모듈 (`lab-space/domain`) 은 `Auth` / `SystemRole` 을 직접 import 하지 않는다 — cross-domain 의존을 `UserId` (EntityId) 한 종류로 환원 (`architecture.md` "도메인 자체 access control 컨셉" 참조).

## 컨트롤러 테스트 — `ControllerDescribeSpec` 기반

본 프로젝트의 컨트롤러 테스트는 `lab-api-support` 의 `ControllerDescribeSpec` 을 상속한다. 모듈 단위의 베이스 한 개(예: `SpaceAppControllerDescribeSpec`) 를 두어 `argumentResolvers`·`controllerAdvices` 를 wiring 하고, 각 컨트롤러 spec 이 그 베이스를 상속해 `when` / `then` / `document` DSL 로 작성한다. 도메인별로 wiring 차이(인증 게이트, advice)가 실제로 생기기 전까지는 한 베이스를 공유한다 — `<Domain>ControllerDescribeSpec` 류의 alias 두 개를 두면 변경 비용만 늘어난다.

```kotlin
class SpaceRegisteringControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Space", body = {
        val useCase = mockk<SpaceRegistering>()
        val controller = SpaceRegisteringController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 생성") {
            it("정상 생성 시 201 과 spaceId 를 반환한다") {
                every { useCase.perform(any()) } returns Result(spaceId = "42")

                controller.`when`(
                    post("/v1/spaces")
                        .withAuth()
                        .body(mapOf("name" to "팀 위키", "description" to "공유"))
                ).then(
                    status().isCreated,
                    jsonPath("$.spaceId").value("42")
                ).document(
                    authHeaderRequired(),
                    requestFields {
                        "name".string("스페이스 이름")
                        "description".string("스페이스 설명")
                    },
                    responseFields {
                        "spaceId".string("생성된 식별자")
                    }
                )
            }
        }
    })
```

### 핵심 원칙

- **standalone MockMvc** — `@WebMvcTest` 슬라이스나 `@SpringBootTest` 컨텍스트 없이 `MockMvcBuilders.standaloneSetup` 으로 controller 인스턴스를 직접 띄운다. spec 부팅이 가볍고, `argumentResolvers`/`controllerAdvices` 가 명시적으로 wiring 된다.
- **OpenAPI 메타데이터는 자동** — `tag` 는 `ControllerDescribeSpec` 생성자에서 받은 값(`"Space"` 등) 이 모든 endpoint 에 자동 적용. `description` 은 `describe(...)` 의 문자열이 자동으로 들어가, `"스페이스 생성"`/`"스페이스 단건 조회"` 같은 한국어 그룹핑이 Swagger UI 에 그대로 노출된다. `summary` 를 따로 명시할 필요 없음.
- **DSL 만으로 OpenAPI 산출** — `requestFields {}` / `responseFields {}` / `pagingParameters()` / `authHeaderRequired()` 같은 헬퍼만 `document(...)` 에 넘기면 `openapi3.json` 에 path parameter·header·request body·response body 가 자동으로 박힌다.
- **인증 stub** — production `AuthArgumentResolver` 는 SessionService + UserRepository 를 의존해서 controller test 마다 wiring 하면 무거워진다. `lab-user/app` 의 testFixtures 가 `StubAuthArgumentResolver` 를 제공해 `Authorization: Bearer userId:role` 포맷의 토큰을 직접 파싱한다 — production 과 동일 헤더 shape, 가벼움. domain 의 `<Domain>AppControllerDescribeSpec` 베이스가 `argumentResolvers = listOf(StubAuthArgumentResolver())` 로 등록. `withAuth(userId, role)` 확장이 헤더를 세팅한다. 토큰 형식 / Redis 세션 / DB user 검증의 회귀는 `AuthArgumentResolverTest` (lab-user/app) 가 책임.
- **mock 라이프사이클** — spec 단위 mock 을 사용할 때는 반드시 `beforeEach { clearMocks(useCase) }` — invocation 누적이 다른 case 의 `verify(exactly = N)` 를 흔든다.
- **path variable 분리** — `get("/v1/spaces/{spaceId}", 1)` 처럼 URI 템플릿 + vararg 로 호출. `get("/v1/spaces/1")` 처럼 hardcoded 하면 산출된 `paths` 키가 `/v1/spaces/1` 로 굳어진다.
- **document 는 정상 케이스만** — 4xx/5xx 회귀(헤더 누락, NotFound 등) 는 `.then(status().isBadRequest)` + `verify(exactly = 0) { useCase.perform(any()) }` 로 마무리. document 산출은 정상 응답 한 케이스로 충분.

### `FieldBuilder` DSL

```kotlin
responseFields {
    "spaceId".string("스페이스 식별자")
    "name".string("이름")
    "description".string("설명", optional = true)
    "createdAt".datetime("생성 시각")
    "members".array("구성원 목록") {
        "id".number("식별자")
        "name".string("이름")
    }
}
```

- `string` / `number` / `boolean` / `datetime` — 기본 타입. 모두 `String.method(description, optional = false)` 형태로 receiver 가 path.
- `array(description) { ... }` / `object(description) { ... }` — nested 구조. children 안에서 path prefix 자동 합성.
- `ignoreBody = true` — subsection 으로 본문 구조 무시(외부 API 의 dynamic payload 등).
- `period()` — `from`/`to` 두 string 필드의 단축형. 도메인 무관한 공통 패턴만 헬퍼로 둔다. aggregate 특화 헬퍼(가격·이미지·주소 류) 는 호출 빈도가 높아진 시점에 헬퍼로 추출.

### `successfulUseCase()` 헬퍼

```kotlin
val useCase: SpaceRegistering = successfulUseCase<SpaceRegistering, Request, Result> {
    Result(spaceId = "42")
}
```

`mockk<T>() + every { perform(any<R>()) } returns ...` 보일러플레이트를 한 줄로 줄인다. `Unit` 반환 UseCase 는 인자 없이 `successfulUseCase<SpaceDeleting, Request>()`.

## 자주 빠뜨리는 것

- **Result 를 다시 감싸기** — `ApiResponse(data = result, message = "ok")` 같은 envelope 은 정말 필요할 때만 도입. 도입한다면 전 계층 일관되게.
- **controller 안에서 권한 체크** — `auth.userId == page.authorId` 같은 검증은 UseCase `validate` 의 일. controller 는 `auth.userId` 를 넘기기만.
- **Body 가 `Request` 자체** — JSON 매핑 어노테이션이 도메인 변환 클래스에 섞이면 경계가 무너진다. Body 는 별도 `data class`.
- **path 와 Request 필드 이름 불일치** — `@PathVariable pageId` 와 Request `pageId` 는 같은 이름으로. 변환은 무명 인자가 아닌 named argument 로.
- **테스트가 service 단위에서 끝남** — controller 테스트가 routing/serialization 까지 검증하지 않으면 path 오타·status 오류가 빠진다 (`conventions.md` "테스트가 실제 회귀를 잡는가").
