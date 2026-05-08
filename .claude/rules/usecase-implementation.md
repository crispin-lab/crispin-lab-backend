# UseCase 구현체 패턴

> **이 문서의 범위**: inbound port 의 `Request` 가 들어왔을 때 **어떤 모양으로 처리할지** 의 canonical 흐름.
>
> **Request/Result 타입 정의**: `usecase-request.md`
> **모듈 경계**: `architecture.md`
> **에러 메시지**: `error-messages.md`

## perform 의 역할

`perform` 은 **비즈니스 흐름의 목차**다. 세부 판단·분기는 private 확장 함수로 내려, `perform` 만 읽고도 흐름이 추적되어야 한다.

### 단계 분리

| 단계 | 책임 | 형태 |
|------|------|------|
| `validate` | 외부 의존성이 필요한 검증 (권한, 엔티티 존재 등) | `Request.also { it.validate() }` |
| `toEntity` | 엔티티 조회 또는 생성 | `Request.toEntity()` |
| `save` | (생성/수정) 영속화 | `Entity.save()` |
| `toResult` | 도메인 → 외부 계약 변환 | `Entity.toResult()` |

엔티티 `init` 의 형식 검증, 엔티티 메서드의 상태 검증은 **이미 끝난 것** 으로 본다 (`conventions.md` 검증 책임 분리 참조). `validate` 는 외부 의존이 필요한 검증만.

## Getting (조회)

```kotlin
package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.page.PageGetting
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.incoming.page.PageGetting.Result
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.domain.page.Page

class PageGettingUseCase(
    private val pageRepository: PageRepository,
) : PageGetting {
    override fun perform(request: Request): Result =
        request
            .also { it.validate() }
            .toEntity()
            .toResult()

    private fun Request.validate() {
        // 외부 의존 검증이 필요할 때만 채운다. 없으면 함수 자체를 두지 않는다.
    }

    private fun Request.toEntity(): Page =
        pageRepository.findBy(pageId)
            ?: throw NotFoundException("페이지를 찾을 수 없습니다.")

    private fun Page.toResult(): Result =
        Result(
            pageId = id.value,
            title = title,
            visibility = visibility.name,
            updatedAt = updatedAt,
        )
}
```

## Registering (생성)

```kotlin
package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.id.IdGenerator
import com.crispinlab.space.application.port.incoming.page.PageRegistering
import com.crispinlab.space.application.port.incoming.page.PageRegistering.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageId.Companion.asPageId

class PageRegisteringUseCase(
    private val pageRepository: PageRepository,
    private val idGenerator: IdGenerator,
) : PageRegistering {
    override fun perform(request: Request) {
        request
            .also { it.validate() }
            .toEntity()
            .let { pageRepository.save(it) }
    }

    private fun Request.validate() {
        // 권한, 상위 페이지 존재 여부 등 외부 의존 검증
    }

    private fun Request.toEntity(): Page =
        Page(
            id = idGenerator.generate().asPageId(),
            title = title,
            body = body,
            visibility = visibility,
            authorId = currentUserId,
        )
}
```

차이점:
- `Result` 가 없다 (반환 `Unit`).
- ID 는 `IdGenerator` 로 생성 (`lab-common` 의 `SnowflakeIdGenerator`).
- 마지막은 `save`. perform 안에 `.let { repository.save(it) }` 로 흐름이 끊기지 않게.

## Modifying (수정)

```kotlin
class PageEditingUseCase(...) : PageEditing {
    override fun perform(request: Request): Result =
        request
            .also { it.validate() }
            .toEntity()
            .editWith(request)
            .let { pageRepository.save(it) }
            .toResult()

    private fun Request.toEntity(): Page =
        pageRepository.findBy(pageId)
            ?.takeIf { it.authorId == currentUserId }
            ?: throw NotFoundException("페이지를 찾을 수 없습니다.")

    private fun Page.editWith(request: Request): Page =
        apply {
            edit(
                title = request.title,
                body = request.body,
                visibility = request.visibility,
            )
        }
}
```

- 권한 체크는 `findBy` + `takeIf` 로 묶어, "존재" 와 "권한" 의 응답을 구분하지 않는다 (`error-messages.md` "정보 노출 방지" 참조).
- 변경은 엔티티 메서드(`edit`) 안에서 — UseCase 가 필드를 직접 대입하지 않는다.

## 트랜잭션 경계

> 본 저장소는 Spring + Exposed 기반. 첫 UseCase 가 들어올 때 한 번에 정한다 — 본 룰은 의도적으로 비워둠.

후보:
- Spring `@Transactional` (서비스 클래스에 부착)
- Exposed `transaction { }` 블록을 perform 진입에 두기
- 별도 `TransactionProvider` 추상화

선택 후 본 문서에 한 줄 추가.

## perform 작성 시 자주 빠뜨리는 것

- **세부 분기를 perform 에 노출** — `if (tokens.isNotEmpty()) ...` 같은 사전 조건은 호출되는 함수 내부로. perform 은 흐름만.
- **확장 함수를 만들지 않고 지역 변수 남발** — 네 줄 이상 길어지면 확장 함수.
- **`validate` 안에서 엔티티를 다시 조회** — 그 조회는 `toEntity` 의 책임. 중복 조회 금지.
- **`toResult` 안에서 추가 DB 조회** — 트랜잭션 경계 밖에서 실행되면 LazyInitialization·세션 클로즈 류 사고. 필요하면 `toEntity` 단계에서 같이 가져온다.
- **`return` 으로 perform 을 끝내는 형태** — 표현식 본문(`= request.also { ... }.toEntity().toResult()`) 으로 두면 흐름이 한 식으로 읽힌다.
- **`?: throw` 의 메시지에 ID·경로 포함** — `error-messages.md` "정보 노출 방지" 참조.
