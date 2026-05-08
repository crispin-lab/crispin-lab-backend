# 도메인 Entity 패턴

> **이 문서의 범위**: `lab-{domain}/domain` 안의 도메인 엔티티/값 객체 정의 형태.
>
> **모듈 경계**: `architecture.md` (`lab-space/domain` 에 Spring/Exposed/HTTP import 금지)
> **타입 일반 규칙**: `conventions.md` "타입 안정성"

## 핵심 규칙

1. **EntityId 는 `data class`** — `equals`/`hashCode`/destructuring 이 그대로 필요. mockk 호환성도 좋다.
2. **Entity 는 일반 `class`** (data class 아님) — 식별자 동치성과 불변성 제어를 직접 잡는다.
3. **모든 `var` 는 `private set`** — 외부에서 필드 직접 변경 금지. 변경은 명시적 메서드로.
4. **상태 변경은 도메인 메서드로만** — `entity.title = "x"` 가 아니라 `entity.rename("x")`.
5. **값 객체(Value Object) 는 `data class`** — Money, EmailAddress 등 불변 보장이 필요한 작은 타입.
6. **`init` 은 형식·길이·빈 값 검증** — 외부 의존이 필요한 검증은 UseCase 책임 (`conventions.md` "검증 책임 분리").

## EntityId

```kotlin
package com.crispinlab.space.domain.page

@JvmInline
value class PageId(val value: Long) {
    companion object {
        fun String.asPageId(): PageId =
            PageId(toLongOrNull() ?: throw IllegalArgumentException("페이지 ID 형식이 올바르지 않습니다."))
    }
}
```

- `value class` (`@JvmInline`) 또는 `data class(val value: Long)`. 단일 값을 감싸는 작은 타입은 `value class` 가 박싱을 줄여준다.
- 변환 함수는 **명사형 `asXxx()`** (`conventions.md` "값 획득 메서드는 명사형"). `toXxx`/`getXxx` 금지.
- 변환 실패는 `IllegalArgumentException`. UseCase Request 단계에서 실패하면 controller 가 400 으로 응답하도록 매핑.

## Entity

```kotlin
package com.crispinlab.space.domain.page

import com.crispinlab.space.domain.user.UserId
import java.time.Instant
import java.time.Instant.now

class Page(
    val id: PageId,
    val authorId: UserId,
    title: String,
    body: String,
    visibility: Visibility,
    val createdAt: Instant = now(),
    updatedAt: Instant = createdAt,
) {
    var title: String = title
        private set
    var body: String = body
        private set
    var visibility: Visibility = visibility
        private set
    var updatedAt: Instant = updatedAt
        private set

    init {
        require(title.isNotBlank()) {
            "제목을 입력해 주세요."
        }
        require(title.length <= MAX_TITLE_LENGTH) {
            "제목은 ${MAX_TITLE_LENGTH}자를 넘을 수 없습니다."
        }
    }

    fun edit(
        title: String? = null,
        body: String? = null,
        visibility: Visibility? = null,
    ) {
        title?.also {
            require(it.isNotBlank()) {
                "제목을 입력해 주세요."
            }
            this.title = it
        }
        body?.also { this.body = it }
        visibility?.also { this.visibility = it }
        updatedAt = now()
    }

    fun publish() {
        check(visibility == Visibility.DRAFT) {
            "이미 발행된 페이지는 다시 발행할 수 없습니다."
        }
        visibility = Visibility.PUBLIC
        updatedAt = now()
    }

    companion object {
        const val MAX_TITLE_LENGTH: Int = 200
    }
}
```

### 작성 시 따져볼 것

- 변경 가능한 필드만 `var`. 식별자·생성 시점 같이 영원히 안 바뀌는 값은 `val` 로 생성자에 둔다.
- 부분 변경 메서드(`edit`) 는 `null = no change` 로 받는다. 이렇게 두면 호출 측에서 부분 업데이트가 자연스러움.
- 상태 전이(`publish`, `archive`) 는 별도 메서드 — `check` 로 상태 가드.
- `updatedAt` 은 변경이 일어난 메서드 안에서 직접 갱신 (자동 콜백 신뢰 X).

## 값 객체 (Value Object)

```kotlin
package com.crispinlab.space.domain.page

data class PageLink(
    val type: Type,
    val target: String,
) {
    init {
        require(target.isNotBlank()) {
            "링크 대상이 비어 있습니다."
        }
    }

    enum class Type {
        INTERNAL, EXTERNAL;

        companion object {
            fun String.asType(): Type =
                entries.firstOrNull { it.name == uppercase() }
                    ?: throw IllegalArgumentException("지원하지 않는 링크 타입입니다.")
        }
    }
}
```

- `data class` — 불변 + 자동 동치성.
- 하위 enum 의 이름은 부모 컨텍스트 안에서 짧게 (`PageLink.Type`, not `PageLinkType`. `conventions.md` "하위 클래스 타입명 축약").
- 외부 입력 변환은 enum companion 안 `asType()` 으로 묶어 둔다.

## Aggregate 경계

`Page` 가 `PageRevision`, `Comment`, `Tag` 같이 별도 lifecycle 을 가지는 객체를 가질 때 — 같은 aggregate 인지 판단:

| 같은 aggregate | 다른 aggregate |
|----------------|----------------|
| 부모 없이는 의미가 없다 (예: `PageRevision`) | 독립 lifecycle (예: `Comment` — Page 삭제 후에도 별도 정책으로 보관 가능) |
| 부모 메서드를 통해서만 변경된다 | 자체 UseCase / Repository 가 있다 |

같은 aggregate 면 부모 entity 의 컬렉션으로, 다른 aggregate 면 별도 Repository/UseCase. `project-context.md` 가 이미 `Comment` 를 별도 aggregate 로 명시함 — 따른다.

## 자주 빠뜨리는 것

- **`data class Page(...)` 로 정의** — `copy()` 가 `private set` 을 우회한다. 일반 `class` 로.
- **`var title: String` (`private set` 누락)** — 외부에서 직접 대입 가능해지면 도메인 메서드 보장이 깨진다.
- **`init` 에서 외부 호출** — Repository 조회·HTTP 호출 등 외부 의존 검증은 UseCase 로. entity 는 **자기 자신만 보고** 검증.
- **상태 전이 메서드 안에서 `if (status == X) return`** — 사일런트 무시 대신 `check(status == X)` 로 명시적으로 실패시킨다. 호출 측 버그가 드러난다.
- **`updatedAt` 갱신 누락** — 변경 메서드마다 직접 `updatedAt = now()`. `edit` 안에서 분기마다 빠뜨리지 않게, 메서드 끝에 한 번 두는 패턴이 무난.
- **enum 이름과 도메인 의미 불일치** — `Visibility.PUBLIC` 같은 도메인 용어 그대로. `STATUS_1` / `OPEN` 같이 모호한 이름 금지 (`conventions.md` "도메인 의미를 먼저 드러낸다").
