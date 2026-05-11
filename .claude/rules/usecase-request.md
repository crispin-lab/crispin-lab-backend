# UseCase Request 패턴

> **이 문서의 범위**: inbound port (UseCase) 의 `Request` / `Result` 타입을 정의하는 **canonical 패턴**.
>
> **일반 코딩 컨벤션**: `conventions.md` 참조
> **모듈 경계**: `architecture.md` 참조

## 핵심 규칙

1. `Request` 는 **`class`** (data class 아님). 생성자에서 primitive → 도메인 타입 변환을 강제하기 위함.
2. 생성자 파라미터는 외부 입력 형태(primitive, String, 외부 enum 코드 등)로 받는다.
3. 본문에서 `val` 프로퍼티로 도메인 타입으로 변환해 노출한다 — 호출부는 도메인 타입으로만 참조한다.
4. **중첩 클래스(예: Attachment)도 같은 패턴**을 따른다. primitive in → 도메인 out.
5. `Result` 는 `data class`. primitive 또는 `lab-common` 의 값 객체만 노출한다 — 도메인 엔티티 직접 노출 금지.
6. 외부 입력 검증은 변환 시점(`asPageId()`, `asUrl()` 등) 또는 `init` 블록의 `require` 로.

## Canonical 예제

```kotlin
package com.crispinlab.space.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.page.PageEditing.Request
import com.crispinlab.space.application.port.incoming.page.PageEditing.Result
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.page.PageLink.Type
import com.crispinlab.space.domain.page.PageLink.Type.Companion.asType
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.page.Visibility.Companion.asVisibility
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.domain.user.UserId.Companion.asUserId
import java.net.URI
import java.time.Instant

interface PageEditing : UseCase<Request, Result> {
    class Request(
        pageId: String,
        val title: String,
        val body: String,
        visibility: String,
        val expectedReviewAt: Instant? = null,
        val links: List<Link>? = null,
        currentUserId: String,
    ) {
        val pageId: PageId = pageId.asPageId()
        val visibility: Visibility = visibility.asVisibility()
        val currentUserId: UserId = currentUserId.asUserId()

        class Link(
            type: String,
            url: String,
            val label: String,
        ) {
            val type: Type = type.asType()
            val url: URI = URI.create(url)
        }
    }

    data class Result(
        val pageId: String,
        val title: String,
        val visibility: String,
        val updatedAt: Instant,
    )
}
```

## 패턴이 보장하는 것

| 효과 | 메커니즘 |
|------|---------|
| 도메인 내부는 String/primitive 가 흘러다니지 않는다 | 생성자에서 즉시 변환 |
| 잘못된 값은 가능한 한 일찍 거른다 | `asPageId()` / `asVisibility()` 가 실패하면 Request 생성 자체가 실패 |
| Request 가 `data class` 였다면 발생할 `copy()` 우회를 막는다 | 일반 `class` 라 copy 가 없음 — 변환 로직을 거치지 않은 인스턴스가 만들어질 수 없음 |
| 중첩 객체도 같은 보장을 받는다 | `Link` 같은 sub class 도 동일 패턴 |
| 호출부 import 가 깔끔하다 | nested class (`Request`, `Result`) 도 직접 import |

## 호출 측 (controller) 변환

controller 는 외부 스펙(JSON body, path variable) 을 그대로 받아 **primitive 형태로** Request 생성자에 넘긴다 — 변환은 Request 가 책임진다.

```kotlin
@PostMapping("/v1/pages/{pageId}")
fun edit(
    @PathVariable pageId: String,
    @RequestBody body: Body,
    auth: Auth,
): Result =
    Request(
        pageId = pageId,
        title = body.title,
        body = body.body,
        visibility = body.visibility,
        expectedReviewAt = body.expectedReviewAt,
        links = body.links?.map { Request.Link(type = it.type, url = it.url, label = it.label) },
        currentUserId = auth.userId,
    ).let { useCase.perform(it) }
```

## 자주 빠뜨리는 것

- **`data class` 로 만들고 변환 프로퍼티를 추가한 케이스** — `copy()` 가 변환을 우회한다. 일반 `class` 로.
- **변환 함수 이름이 `getXxx` / `toXxx` 형태** — 값 획득 메서드는 명사형(`asPageId`)으로. `conventions.md` "값 획득 메서드는 명사형" 참조.
- **Result 가 도메인 엔티티 자체를 노출** — Result 는 외부 계약. primitive / 값 객체로만.
- **중첩 클래스만 `data class` 로 두는 혼합** — 동일 규칙을 적용한다. 외부 → 도메인 변환이 필요하면 일반 `class`.
- **변환 함수를 Request `init` 블록에 모아두는 형태** — 프로퍼티 초기화로 자연스럽게 선언적으로 표현 가능. `init` 은 cross-field 검증(`require`) 같은 진짜 추가 검증에만 쓴다.
