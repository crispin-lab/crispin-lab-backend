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

## Result 축소 원칙

도메인 UseCase Result 는 **자기 도메인 데이터 + 크로스도메인 identifier (EntityId) 만** 노출한다. `authorHandle` (user 의 handle), `authorSpaceMemberships` (user × space 관계) 같은 **다른 도메인 파생 스칼라를 도메인 Result 에 넣지 않는다**. 조립은 `lab-composition/app` 의 BFF controller 가 담당 — 도메인 Result 의 `authorId` 를 받아 `UserHandleLookup.handleOf(authorId)` 로 조회해 Payload 에 실는다.

| 도메인 Result 에 두는 것 | 도메인 Result 에 두지 않는 것 |
|--------------------------|-------------------------------|
| 자기 도메인 필드 (`title`, `visibility`, `createdAt`) | 다른 도메인의 파생 스칼라 (`authorHandle`) |
| 자기 도메인 identifier (`pageId`, `spaceId`) | 다른 도메인의 파생 컬렉션 (`memberSpaceIds: Set<SpaceId>`) |
| 다른 도메인의 identifier (`authorId: UserId`) | 자기 UseCase 로직에 필요 없는 재계산 값 |
| 자기 도메인 안에서 viewer × entity 로 결정되는 파생 boolean (`canEdit`, `canComment`) | — |

viewer 파생 boolean 은 cross-domain 데이터가 아니라 **자기 도메인의 access 정책** 이 만들어내는 값이라 도메인 Result 에 두는 것이 정합 — BFF 로 옮기면 access 정책이 도메인 밖으로 새어나가 계층 경계가 무너진다.

### 이유 — cycle 차단

`PageGettingUseCase` 가 `UserHandleQuery` 를 주입받아 `authorHandle` 을 채우는 형태로 두면 `lab-space/app → lab-user/domain` 의 read-side 의존이 늘고, 필드 확장이 반복되면 결국 반대 방향 (user 가 space 를 읽는 케이스) 이 필요해질 때 gradle module cycle 로 이어진다. 도메인 Result 를 identifier 만으로 축소하면 도메인 module 은 "handle 조회 무지" 상태를 유지하고, cycle 여지 자체가 없다. 자세한 배경은 `architecture.md` "BFF/Composition 계층 — 도입 배경".

### 예시 대조

```kotlin
// BAD — 도메인 Result 에 다른 도메인 파생 스칼라
interface PageGetting : UseCase<Request, Result> {
    data class Result(
        val pageId: PageId,
        val title: String,
        val authorId: UserId,
        val authorHandle: String,
    )
}

// GOOD — identifier 만. BFF 가 조립.
interface PageGetting : UseCase<Request, Result> {
    data class Result(
        val pageId: PageId,
        val title: String,
        val authorId: UserId,
    )
}
```

BFF (`lab-composition/app`) 의 controller 가 Result 를 받아:

```kotlin
private fun Result.toPayload(): PagePayload =
    PagePayload(
        pageId = pageId,
        authorId = authorId,
        authorHandle = userHandleLookup.handleOf(authorId),
        title = title,
    )
```

조립 세부 (단건 / 리스트 / write) 는 `controller.md` "크로스도메인 조립 응답은 BFF 계층에서" 참조.

### 예외 — write 흐름의 도메인 사이드 이펙트

`MentionDispatcher` 처럼 write UseCase 안에서 다른 도메인의 read 를 쓰지만 **정책 판단 자체가 자기 도메인 소유** 인 경우는 이 원칙과 무관하다 — Result 를 축소하는 게 아니라 도메인 안의 부수효과라 그대로 도메인 module 에 남는다 (`architecture.md` "BFF/Composition 계층 — 예외 케이스").

## Getting (조회)

```kotlin
package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageGetting
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.incoming.page.PageGetting.Result
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.domain.page.Page

class PageGettingUseCase(
    private val pageRepository: PageRepository,
    private val transactionProvider: TransactionProvider,
) : PageGetting {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .also { it.validate() }
                .toEntity()
                .toResult()
        }

    private fun Request.validate() {
        /*
        todo    :: 권한·존재 등 외부 의존 검증을 둘 자리. 비어 있어도 perform 흐름 정렬을 위해 유지.
         author :: <사람 이름>
         date   :: YYYY-MM-DDTHH:mm:ssKST
         ticket :: LAB-N
         */
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
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageRegistering
import com.crispinlab.space.application.port.incoming.page.PageRegistering.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageId.Companion.asPageId

class PageRegisteringUseCase(
    private val pageRepository: PageRepository,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider,
) : PageRegistering {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            request
                .also { it.validate() }
                .toEntity()
                .let { pageRepository.save(it) }
        }
    }

    private fun Request.validate() {
        /*
        todo    :: 권한, 상위 페이지 존재 여부 등 외부 의존 검증.
         author :: <사람 이름>
         date   :: YYYY-MM-DDTHH:mm:ssKST
         ticket :: LAB-N
         */
    }

    private fun Request.toEntity(): Page =
        Page(
            id = idGenerator.next().asPageId(),
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
class PageEditingUseCase(
    private val pageRepository: PageRepository,
    private val transactionProvider: TransactionProvider,
) : PageEditing {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .also { it.validate() }
                .toEntity()
                .editWith(request)
                .let { pageRepository.save(it) }
                .toResult()
        }

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

## Deleting (삭제)

```kotlin
class PageDeletingUseCase(
    private val pageRepository: PageRepository,
    private val transactionProvider: TransactionProvider,
) : PageDeleting {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            request
                .toEntity()
                .withdraw()
        }
    }

    private fun Request.toEntity(): Page =
        pageRepository.findBy(pageId)
            ?.takeIf { it.authorId == currentUserId }
            ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)

    private fun Page.withdraw() {
        pageRepository.delete(id)
    }
}
```

- `Result` 없이 `Unit` 반환 (`PageDeleting : UseCase<Request, Unit>`).
- 표준은 **`repository.delete(id)` 한 줄** — entity 가 `SoftDeletable` 이면 어댑터의 base 가 자동 분기로 `UPDATE deleted_at = now()`, 아니면 hard `DELETE` (`repository.md`). entity 에는 `delete()` 도메인 메서드를 두지 않는다 — 이중 삭제 invariant 는 `findBy` 자동 필터가 deleted entity 를 못 찾는 것으로 자연 보호 (NotFoundException 으로 fallback). 미래에 상태 머신·부수효과 같은 추가 invariant 가 필요해지면 그 PR 에서 도메인 메서드와 호출처를 함께 도입 (`entity.md` "SoftDeletable entity 패턴" 참조).
- **`.withdraw()` 확장 함수**로 어댑터 호출을 분리 — `.let { repo.delete(it.id) }` 형태의 `it.id` 추출이 어색하므로 `private fun Entity.withdraw()` 로 한 단어. `withdraw` 는 위키/블로그 도메인에서 "게시물을 내린다" 의 사용자 행위. 본 저장소의 `toEntity` / `toResult` / `editWith` 단계 분리 패턴과 정합.

### 권한 통과 후 멱등 — association / admin-gate 삭제

association entity (예: `PageTag`) 의 `detach` 와 admin-only 삭제 (예: `TagDeleting`) 는 `findBy + takeIf` 로 엔티티 자체를 다시 조회하지 않고, 권한 검증만 한 뒤 `repository.detach(...)` / `repository.delete(id)` 를 호출한다. 매핑이 없거나 이미 삭제된 row 라도 `WHERE` 절이 0 rows affected 로 끝나 응답은 동일 (204). 의도:

- **enumeration 방지**: 매핑 존재 여부 / row 존재 여부를 응답으로 구분하지 않는다 — IDOR/enumeration 정보 누출 없음 (`error-messages.md` "정보 노출 방지" 정합).
- **race 안전**: ADMIN 두 명이 동시에 같은 `delete` 를 요청해도 둘 다 204. "X 가 없도록 만들어 달라" 는 의도라 race 마다 404 분기가 무의미.
- **단건 entity 삭제와 다른 결**: `PageDeleting` / `CommentDeleting` 은 author/admin 검증을 위해 `findBy + takeIf` 가 필요하지만, association / admin-gate 는 권한 검증 자체가 entity 본체에 의존하지 않으므로 추가 조회를 두지 않는다.

## 트랜잭션 경계

`lab-common` 의 `TransactionProvider` 를 UseCase 생성자로 주입받아 **`perform` 진입에서 한 번** 감싼다. 구현체(`DefaultTransactionProvider`) 는 `lab-common-infra` 가 Spring Boot auto-config 로 등록하므로 app 모듈은 의존만 추가하면 된다.

```kotlin
class PageEditingUseCase(
    private val pageRepository: PageRepository,
    private val transactionProvider: TransactionProvider,
) : PageEditing {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .also { it.validate() }
                .toEntity()
                .editWith(request)
                .let { pageRepository.save(it) }
                .toResult()
        }
}
```

### 규칙

- **경계는 perform 진입 한 번**. `validate` → `toEntity` → `save` → `toResult` 전체가 한 트랜잭션 안에서 흐른다. `toResult` 안의 추가 조회도 같은 세션에서 안전.
- **조회 전용 UseCase 는 `transactional(readOnly = true) { ... }`**. 쓰기 동반(Registering, Editing, Deleting) 은 기본값(false).
- **도메인 예외(NotFoundException 등) 도 자동 롤백**된다 — `RuntimeException` 이므로 `TransactionTemplate` 가 롤백 처리. 별도 옵션 불필요.
- 어댑터(`ExposedPageRepository` 등) 는 현재 트랜잭션을 가정하고 `transaction { ... }` 블록을 다시 열지 않는다 (`repository.md` 참조).
- propagation / isolation / after-commit 콜백이 필요해지면 그때 시그니처를 확장한다 — 지금은 `readOnly` 만.

### 테스트

- UseCase 단위 테스트에서는 `lab-space/app/testsupport` 의 `DummyTransactionProvider` (block 을 그대로 실행) 를 주입해 트랜잭션 매니저 없이 검증한다. (첫 UseCase 티켓에서 testsupport 추가)
- `DefaultTransactionProvider` 자체의 회귀(commit/rollback/readOnly) 는 `lab-common-infra` 의 통합 테스트가 책임진다.

## 권한 검증 (Viewer + ForbiddenException + IDOR)

권한 정책은 controller 가 매핑한 도메인 자체 access type (예: `Viewer`) 을 Request 가 받아 UseCase 가 분기하는 형태로 짠다. cross-domain 의존을 `UserId` (EntityId) 한 종류로 환원하고, role 분류 같은 user-domain 의 개념은 어댑터 경계에 머문다 (`controller.md` "Auth → 도메인 자체 access type 변환" / `architecture.md` "도메인 자체 access control 컨셉" 참조).

### ADMIN gate — `ForbiddenException`

자원의 존재 자체가 클라이언트에게 노출되어도 무방한 admin-only 동작 (예: `SpaceRegistering` / `SpaceEditing` / `SpaceDeleting`) 은 `validate()` 에서 명시적 403 으로 차단:

```kotlin
private fun Request.validate() {
    if (!viewer.isAdmin) {
        throw ForbiddenException(SpaceErrorCode.SPACE_ADMIN_ONLY)
    }
}
```

Request 시그니처는 `viewer: Viewer.Member` (sealed variant 직접) — Anonymous 케이스 자체가 표현 불가, controller 가 인증 필수 endpoint 에서만 호출됨.

### IDOR 보호 — `findBy + takeIf` 통합

자원의 존재가 권한에 따라 다르게 노출되어야 하는 경우 (예: `PageEditing` / `PageDeleting` — 다른 사용자의 DRAFT) 는 ForbiddenException 으로 분리하지 않고 NotFoundException 으로 통합한다 (`error-messages.md` 정합):

```kotlin
private fun Request.toEntity(): Page =
    pageRepository
        .findBy(pageId)
        ?.takeIf { viewer.isAdmin || it.authorId == viewer.userId }
        ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
```

### Visibility 정책 — sealed type 으로 단일 진실

PUBLIC / INTERNAL / DRAFT 같이 자원 자체에 visibility 가 있는 경우, 단건 조회 (`Getting`) 와 검색 (`Searching`) 두 곳에 정책이 중복 인코딩되기 쉽다. **port 의 sealed type 안에 `allows(visibility, authorId)` 같은 정책 함수를 두고 두 UseCase 가 같은 함수를 호출**하게 한다.

```kotlin
// PageSearchPort
sealed interface VisibilityScope {
    fun allows(visibility: Visibility, authorId: UserId): Boolean

    data object Anonymous : VisibilityScope { ... }
    data class Authenticated(val viewerId: UserId) : VisibilityScope { ... }
    data object Privileged : VisibilityScope { ... }

    companion object {
        fun of(viewer: Viewer): VisibilityScope = ...
    }
}

// PageGettingUseCase (단건)
val scope = VisibilityScope.of(viewer)
pageRepository.findBy(pageId)
    ?.takeIf { scope.allows(it.visibility, it.authorId) }
    ?: throw NotFoundException(...)

// PageSearchingUseCase (검색)
pageSearchPort.search(..., scope = VisibilityScope.of(viewer), ...)
```

이렇게 두면 SQL 어댑터 (검색) 와 메모리 단건 체크가 같은 sealed type 위에서 결정되어, 한 정책 변경이 두 흐름에 자동 전파된다. 정책이 단순한 경우 (예: Space 의 PUBLIC/INTERNAL 만) 는 sealed type 까지 가지 않고 같은 application 패키지의 internal helper (`Viewer.allowedSpaceVisibilities(): Set<SpaceVisibility>`) 한 곳에 두는 것으로 충분.

## perform 작성 시 자주 빠뜨리는 것

- **세부 분기를 perform 에 노출** — `if (tokens.isNotEmpty()) ...` 같은 사전 조건은 호출되는 함수 내부로. perform 은 흐름만.
- **확장 함수를 만들지 않고 지역 변수 남발** — 네 줄 이상 길어지면 확장 함수.
- **`validate` 안에서 엔티티를 다시 조회** — 그 조회는 `toEntity` 의 책임. 중복 조회 금지.
- **`toResult` 안에서 추가 DB 조회** — 트랜잭션 경계 밖에서 실행되면 LazyInitialization·세션 클로즈 류 사고. 필요하면 `toEntity` 단계에서 같이 가져온다.
- **`return` 으로 perform 을 끝내는 형태** — 표현식 본문(`= request.also { ... }.toEntity().toResult()`) 으로 두면 흐름이 한 식으로 읽힌다.
- **`?: throw` 의 메시지에 ID·경로 포함** — `error-messages.md` "정보 노출 방지" 참조.
