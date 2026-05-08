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

## 자주 빠뜨리는 것

- **Result 를 다시 감싸기** — `ApiResponse(data = result, message = "ok")` 같은 envelope 은 정말 필요할 때만 도입. 도입한다면 전 계층 일관되게.
- **controller 안에서 권한 체크** — `auth.userId == page.authorId` 같은 검증은 UseCase `validate` 의 일. controller 는 `auth.userId` 를 넘기기만.
- **Body 가 `Request` 자체** — JSON 매핑 어노테이션이 도메인 변환 클래스에 섞이면 경계가 무너진다. Body 는 별도 `data class`.
- **path 와 Request 필드 이름 불일치** — `@PathVariable pageId` 와 Request `pageId` 는 같은 이름으로. 변환은 무명 인자가 아닌 named argument 로.
- **테스트가 service 단위에서 끝남** — controller 테스트가 routing/serialization 까지 검증하지 않으면 path 오타·status 오류가 빠진다 (`conventions.md` "테스트가 실제 회귀를 잡는가").
