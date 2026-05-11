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

`adapter/web/auth/AuthArgumentResolver` 가 `X-User-Id` 헤더를 받아 `Auth(userId)` 로 변환한다 (현재 임시 — 토큰 기반 인증 도입 시 같은 ArgumentResolver 만 갈아끼운다). 두 가지 책임을 어댑터 경계에서 끝낸다:

1. **헤더 존재 확인** — 없으면 `IllegalArgumentException("사용자 인증이 필요합니다.")` → 400.
2. **형식 검증** — `toLongOrNull()` 로 숫자 변환 가능 여부만 확인. 실패 시 같은 메시지로 400.

검증을 UseCase Request 의 `asUserId()` 변환에 미루지 않는 이유: 인증 게이트키퍼와 도메인 변환의 책임을 섞으면 응답 코드·메시지의 의도가 흐려진다. `AuthArgumentResolver` 에서 한 번에 닫는다.

> 임시 헤더 인증 단계에서는 두 실패 모두 400(`IllegalArgumentException`) 으로 응답한다. 토큰 인증 도입 PR 에서 401(`Unauthorized`) 로 정정하고 controller 테스트의 `isBadRequest()` 단언도 함께 갱신할 것 — RFC 7235 정합.

## 컨트롤러 테스트 — `ControllerDescribeSpec` 기반

본 프로젝트의 컨트롤러 테스트는 `lab-api-support` 의 `ControllerDescribeSpec` 을 상속한다. 영역마다 `<Domain>ControllerDescribeSpec` (예: `SpaceControllerDescribeSpec`) 을 두어 `argumentResolvers`·`controllerAdvices` 를 wiring 한 뒤, 각 컨트롤러 spec 이 그 베이스를 상속해 `when` / `then` / `document` DSL 로 작성한다.

```kotlin
class SpaceRegisteringControllerTest :
    SpaceControllerDescribeSpec(tag = "Space", body = {
        val useCase = mockk<SpaceRegistering>()
        val controller = SpaceRegisteringController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 생성") {
            it("정상 생성 시 201 과 spaceId 를 반환한다") {
                every { useCase.perform(any()) } returns Result(spaceId = "42")

                controller.`when`(
                    post("/v1/spaces")
                        .withUserHeader()
                        .body(mapOf("name" to "팀 위키", "description" to "공유"))
                ).then(
                    status().isCreated,
                    jsonPath("$.spaceId").value("42")
                ).document(
                    userHeaderRequired(),
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
- **DSL 만으로 OpenAPI 산출** — `requestFields {}` / `responseFields {}` / `pagingParameters()` / `userHeaderRequired()` 같은 헬퍼만 `document(...)` 에 넘기면 `openapi3.json` 에 path parameter·header·request body·response body 가 자동으로 박힌다.
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
