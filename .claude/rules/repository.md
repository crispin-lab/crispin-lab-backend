# Repository / Outbound Port 패턴

> **이 문서의 범위**: outbound port 인터페이스 정의 + Exposed 어댑터 구현 형태.
>
> **모듈 경계**: `architecture.md` (`lab-space/domain` 에 Exposed import 금지 — 어댑터는 `lab-space/app`)
> **Entity 정의**: `entity.md`
> **에러 메시지**: `error-messages.md`

## 개념 구분

`outbound port` 는 두 종류로 나눠 쓴다.

| 타입 | 책임 | 반환 타입 |
|------|------|----------|
| **Repository** | 엔티티 단위 영속화 (ID 기반 CRUD) | 도메인 엔티티 그대로 |
| **Search / Retriever** | 복잡한 조회 (조인, 페이징, 검색) | 자체 정의 Summary/Snapshot |

같은 객체를 어떤 식으로 다룰지에 따라 갈린다 — `pageRepository.findBy(id)` 는 `Page`, `pageSearchPort.findRecent(...)` 는 `PageSummary` 식.

## Repository 인터페이스 (port)

```kotlin
package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageId

interface PageRepository {
    fun save(entity: Page): Page
    fun findBy(id: PageId): Page?
    fun delete(id: PageId)
}
```

- 단건 조회는 `findBy(id)` — `OrNull` suffix 금지 (`conventions.md` "nullable 반환과 메서드명").
- `save` 는 신규/수정 모두 처리 (Exposed 의 `insertOrUpdate` 또는 `id` 존재 분기). 호출부에서 신경 안 쓰게.
  - **race 주의**: SELECT → INSERT 분기 패턴은 같은 ID 의 동시 호출이 들어오면 unique constraint 위반으로 깨진다. snowflake ID 단건 PK 는 충돌 확률이 매우 낮지만, slug 같은 unique 컬럼 기반 분기에는 동일 패턴을 복제하지 말고 `upsert` 로 한 번에 처리한다.
- 삭제는 `delete(id: PageId)`. port 시그니처는 항상 `delete(id)` 한 종류 — `softDelete` / `hardDelete` 류 분리 시그니처 금지.
  - **자동 분기**: entity 가 `SoftDeletable` (`lab-common-domain`) 을 implement 하고 어댑터가 `deletedAtColumn` 을 override 하면 base 의 `delete(id)` 가 `UPDATE deleted_at = now()` 로 동작. 같은 entity 의 `findBy` / `findAllBy` 와 도메인 특화 쿼리는 `notDeleted()` 헬퍼로 `deleted_at IS NULL` 자동 필터.
  - **hard delete 유지**: `deletedAtColumn` 미 override 면 기존 SQL DELETE. Tag / PageRevision / PageLink / PageTag 처럼 종속·association entity 는 hard delete 유지. `Page` 가 soft delete 돼도 `page_tags` 매핑 row 는 보존된다 (`page_tags.page_id` FK 의 CASCADE 가 hard DELETE 가 아닌 한 트리거되지 않음) — page 가 복구되면 tag 매핑도 자연 복원. 단 tag 기반 검색 (`PageSearchPort` 등) 은 join 시 `pages.deleted_at IS NULL` 을 명시 필터해 deleted page 를 노출하지 않는다.
  - **표준 흐름은 `repository.delete(id)` 한 줄**: UseCase 는 `findBy + takeIf` 로 권한/소유자 검증 후 `repository.delete(id)` 호출. 이미 deleted 인 entity 는 `findBy` 자동 필터로 못 찾으니 NotFoundException 으로 fallback — invariant 가 자연스럽게 보호된다 (이중 삭제 시도 차단). `Page`, `Space`, `Comment` 모두 이 표준을 따른다.
  - **`entity.delete() + save` 경로의 자리**: entity 의 `delete()` 도메인 메서드는 미래에 추가 invariant (예: 상태 머신, 부수효과) 가 필요해질 때를 위한 enabler 로 base 가 받아주지만, 본 시점 표준 흐름은 아니다. 사용처 PR 에서 명시적 의도와 함께 도입.
  - **`findIncludingDeleted` 류**: base 에 `findByIncludingDeleted` 가 protected 로 준비되어 있다. admin/복구 UseCase 가 등장하는 시점에 port 한 줄 + 자식 `public override` 한 줄로 expose — 사용처가 없으면 port 에 추가하지 않는다 (`conventions.md` "구현 없는 포트 머지 금지").

## Exposed 테이블 객체

```kotlin
package com.crispinlab.space.adapter.persistence.page

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Pages : Table("pages") {
    val id = long("id")
    val authorId = long("author_id")
    val title = varchar("title", length = 200)
    val content = text("content")
    val visibility = varchar("visibility", length = 20)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
```

- **객체명과 테이블명 일치** — `object Pages : Table("pages")` (`conventions.md` "Exposed 테이블 객체명과 테이블명 일치"). `object PageTable : Table("pages")` 금지.
- 컬럼명은 snake_case. 코드 식별자는 camelCase.
- `varchar` 는 길이를 entity 의 `MAX_*_LENGTH` 상수와 맞춘다.

## Repository 구현 (어댑터)

```kotlin
package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.page.Visibility.Companion.asVisibility
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository

@Repository
class ExposedPageRepository :
    ExposedEntityRepository<Page, PageId>(),
    PageRepository {
    override val table = Pages
    override val idColumn = Pages.id

    override fun ResultRow.toEntity(): Page =
        Page(
            id = PageId(this[Pages.id]),
            authorId = UserId(this[Pages.authorId]),
            title = this[Pages.title],
            content = PageContent(this[Pages.content]),
            visibility = decodeVisibility(this[Pages.visibility]),
            createdAt = this[Pages.createdAt],
            updatedAt = this[Pages.updatedAt]
        )

    @Suppress("RedundantOverride")
    override fun delete(id: PageId) = super.delete(id)

    override fun insert(entity: Page) {
        Pages.insert {
            it[id] = entity.id.value
            it[authorId] = entity.authorId.value
            it[title] = entity.title
            it[content] = entity.content.raw
            it[visibility] = entity.visibility.name
            it[createdAt] = entity.createdAt
            it[updatedAt] = entity.updatedAt
        }
    }

    override fun update(entity: Page) {
        Pages.update({ Pages.id eq entity.id.value }) {
            it[title] = entity.title
            it[content] = entity.content.raw
            it[visibility] = entity.visibility.name
            it[updatedAt] = entity.updatedAt
        }
    }

    private fun decodeVisibility(stored: String): Visibility =
        runCatching { stored.asVisibility() }
            .getOrElse { cause ->
                throw IllegalStateException("저장된 visibility 값을 해석할 수 없습니다.", cause)
            }
}
```

### ExposedEntityRepository base

`lab-space/app/adapter/persistence/ExposedEntityRepository.kt` 에 다음 abstract class 가 있다:

`E` 는 `Entity<I>` 마커를 만족해야 한다 (`entity.md` 참조). base 의 시그니처:

```kotlin
abstract class ExposedEntityRepository<E : Entity<I>, I : EntityId> {
    protected abstract val table: Table
    protected abstract val idColumn: Column<Long>
    protected abstract val deletedAtColumn: Column<Instant?>?     // SoftDeletable 어댑터는 실제 컬럼, 그 외는 null 명시 override

    protected abstract fun ResultRow.toEntity(): E
    protected abstract fun insert(entity: E)
    protected abstract fun update(entity: E)

    fun save(entity: E): E = ...                       // public — port 가 모두 노출. SELECT 분기도 자동 필터 (deleted row 의 silent undelete 차단)
    fun findBy(id: I): E? = ...                        // public — port 가 모두 노출. deletedAtColumn 이 컬럼이면 자동 필터
    protected open fun findAllBy(ids: List<I>): List<E> = ...
    protected open fun delete(id: I) = ...             // deletedAtColumn 이 컬럼이면 UPDATE, null 이면 DELETE

    protected fun notDeleted(): Op<Boolean> = deletedAtColumn?.isNull() ?: Op.TRUE
}
```

- 어댑터는 `table`, `idColumn`, `deletedAtColumn`, `ResultRow.toEntity()`, `insert`, `update` 를 구현. SELECT → insert/update 분기와 findBy/findAllBy/delete 의 SQL 보일러플레이트는 base 가 통합. **`deletedAtColumn` 은 abstract** — SoftDeletable entity 어댑터는 실제 컬럼 (`Pages.deletedAt`) 을, hard delete 어댑터는 `null` 을 명시 override. `open val` + default null 로 두면 base 안에서의 nullability 추론이 IDE 에서 false positive (`Useless null check` / `Useless null-safe access`) 를 띄우므로 abstract 가 정합.
- **노출 범위**: `save` / `findBy` 는 모든 어댑터의 port 가 노출하므로 base 에서 public. `findAllBy` / `delete` 는 `protected open` 으로 두고, port 시그니처가 정의된 어댑터에서만 `@Suppress("RedundantOverride") override fun delete(id: I) = super.delete(id)` 로 명시 expose.

### delete expose 의 두 inspection 함정

자식 어댑터가 두 부모 (`ExposedEntityRepository` 의 `protected delete` + port interface 의 `public delete`) 를 동시에 override 하는 케이스라 IDE inspection 이 두 가지 false positive 를 띄운다 — 둘 다 무시하면 안 되는 의도된 패턴:

1. **`public override` 의 `public` 은 redundant** — Kotlin 의 visibility override 규칙상 두 부모 중 가장 넓은 visibility (public) 가 자동 적용되므로 `public` modifier 가 진짜 redundant. 이건 따라서 제거 (`override` 만 남김).
2. **`override` 자체는 redundant 가 아님** — body 가 `super.delete(id)` 한 줄뿐이라 IntelliJ 가 "Redundant overriding method" inspection 으로 잡지만, 이걸 따라 제거하면 `Cannot weaken access privilege` 컴파일 에러가 난다 (base 의 protected 가 interface 의 public 보다 좁아 자동 inherit 로는 interface 시그니처를 만족 못 시킴). 즉 override 의 **유일한 역할이 visibility widening (protected → public)** 인데 IDE inspection 이 body 만 보고 그 효과를 인식 못 함 (KT-46667 류). `@Suppress("RedundantOverride")` 로 명시 우회 — `conventions.md` "IDE 경고는 무시하지 않는다. 무시해야 하면 `@Suppress` 로 명시" 정합. 이유는 본 룰 문서로 한 곳에 모으고 코드 측 주석은 두지 않는다.

### 왜 base 가 protected 인가

base 가 처음부터 public 이면 `PageRevisionRepository` 처럼 port 에 `delete` 가 없는 어댑터에서도 외부 호출이 가능해져 aggregate 일관성이 깨진다. base 를 protected 로 두면 port 가 명시한 어댑터만 visibility widening 으로 expose 하므로, aggregate 경계가 컴파일러로 보존된다. `findByIncludingDeleted` (admin/복구) 가 필요해지면 같은 패턴 — base 의 protected helper + port 추가 + 자식 expose 를 그 PR 에서 함께 (`conventions.md` "구현 없는 포트/미사용 코드" 정합).
- **자동 필터 헬퍼 `notDeleted()`**: `deletedAtColumn` 이 `null` 이면 `Op.TRUE` 로 풀려 hard delete 어댑터에서도 동일 호출 가능. 자식 어댑터의 도메인 특화 쿼리에서 `where { (Pages.parentPageId eq parentId.value) and notDeleted() }` 같이 한 줄 추가로 일관 필터. 새 도메인 쿼리 작성 시 누락하면 deleted row 가 노출 — PR 체크리스트 항목. base 를 상속하지 않는 별도 어댑터 (예: `ExposedPageSearchAdapter`) 도 동일 정신으로 `Pages.deletedAt.isNull()` 을 직접 명시.
- **`save` 의 SELECT 분기도 `notDeleted()` 로 보호**: 일반 caller 가 deleted entity 를 들고 와 `save` 를 호출하면 base 의 SELECT 가 row 를 못 찾고 `insert(it)` 경로로 흘러 PK 충돌 (`ExposedSQLException`) 로 fail-fast. silent 한 undelete 가 발생하지 않는다. `entity.delete() + save` 같은 도메인 메서드 경로 (미래 invariant 보호 enabler) 는 진입 시점에 row 가 아직 not-deleted 라 정상 update 로 흐른다.
- 어댑터 클래스명 prefix 는 **기술 스택**(`Exposed`) 으로. `MySql`, `Redis` 등도 같은 결.
- 도메인 특화 메서드 (`findByPageId`, `findBySpaceId`, `attach/detach` 등) 는 어댑터에 그대로 둔다 — base 가 일반화하지 않는다.
- 도메인 port Repository 인터페이스 (`PageRepository` 등) 는 **공통 super type 없이** 각자 정의. base 가 강제하는 추상화는 어댑터 측에만.
- `saveAll` / `batchInsert` 만 쓰는 어댑터 (예: `ExposedPageLinkRepository`) 는 base 의 단건 CRUD 가 무의미하므로 base 상속하지 않는다.

### 작성 시 따져볼 것

- 매핑 함수 이름은 `ResultRow.toEntity()` — `from`, `mapToPage` 등 흩뿌리지 말 것. 한 어댑터 안에서 일관.
- enum 은 컬럼에 `name` 으로 저장하고 읽을 때 `asXxx()` 로 복원. 인덱스가 필요하면 별도 정수 컬럼 고려.
- **DB 손상 enum 매핑은 `IllegalStateException` 으로 래핑** — `asVisibility()` 가 `IllegalArgumentException` 을 던지지만, 어댑터에서 그대로 흘리면 `GlobalExceptionHandler` 가 400 으로 매핑한다. DB 에 깨진 값이 들어 있는 건 외부 입력 오류가 아니라 운영 결함이므로 `decodeXxx` 헬퍼로 래핑해 500 으로 응답되게 한다 (`error-messages.md` 의 "외부 입력 / 내부 상태" 구분과 정합).
- `updatedAt` 은 어댑터에서 갱신하지 않는다 — entity 메서드가 이미 갱신했음 (`entity.md` "`updatedAt` 갱신 누락" 참조).
- 도메인 port 의 `save` 파라미터 이름은 `entity` 로 통일 — base 의 `save(entity: E)` 와 시그니처 정합. 다른 이름 (`page`, `space` 등) 을 쓰면 named-argument 경고가 뜬다.

## Search / Retriever (보조 조회)

```kotlin
package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import java.time.Instant

interface PageSearchPort {
    fun findRecent(
        visibility: Visibility,
        pageRequest: PageRequest,
    ): PageResult<PageSummary>

    data class PageSummary(
        val id: PageId,
        val title: String,
        val updatedAt: Instant,
    )
}
```

- Summary 는 **이 port 안의 nested data class** — 이 조회의 결과 형태를 다른 곳에서 재사용하지 않는다.
- 도메인 entity 를 그대로 노출하지 않는다 — 조회 전용 모양은 별도.
- `lab-common` 의 `PageRequest` / `PageResult` 를 그대로 활용.

`project-context.md` 의 "Elasticsearch 기반 검색 — 지금은 인터페이스만, SQL `LIKE` 로 구현" 메모와 정합. SQL 어댑터는 같은 `PageSearchPort` 를 구현하면 ES 어댑터로 교체할 때도 인바운드는 그대로.

## 자주 빠뜨리는 것

- **port 가 `lab-{domain}/domain` 에 들어감** — port 는 `application` 패키지. domain 은 entity / value object 만.
- **Repository 가 도메인 메서드를 호출** — `save` 안에서 `page.publish()` 같은 도메인 동작 호출 금지. 그건 UseCase 책임.
- **`findById` 라는 이름** — `findBy(id: PageId)` 가 본 저장소 컨벤션. 시그니처가 ID 임을 이미 보여준다.
- **컬럼 누락 매핑** — `save` 의 `insert`/`update` 와 `toEntity()` 가 비대칭이면 사일런트 데이터 손실. PR 체크리스트 (필드 추가) 항목으로 묶어 확인.
- **Exposed `transaction { ... }` 을 어댑터 안에서** — 트랜잭션 경계는 UseCase 단계가 가져간다 (`usecase-implementation.md` "트랜잭션 경계"). 어댑터는 현재 트랜잭션을 가정.
- **테이블 객체명 - 테이블명 불일치** — `object PageTable : Table("pages")` 같은 형태. `object Pages : Table("pages")` 로.
- **SoftDeletable 어댑터의 도메인 특화 쿼리에 `notDeleted()` 누락** — base 의 `findBy`/`findAllBy` 만 자동 필터되고, `findChildren`/`findRoots`/`findByPageId` 같은 자식 쿼리는 손수 `and notDeleted()` 를 붙여야 한다. 누락 시 deleted row 가 노출. hard delete 어댑터에서도 호출 가능 (`Op.TRUE` 로 풀림) 하니 패턴 일관성 차원에서 추가해도 무해.
- **`deletedAtColumn` 을 실제 컬럼으로 override 했는데 entity 가 `SoftDeletable` implement 안 함 (또는 반대)** — `toEntity()`/`insert()`/`update()` 에서 `entity.deletedAt` 접근 시 컴파일 에러로 즉시 잡힘. 한쪽만 추가하면 build 가 잡으므로 동시 변경 강제. `deletedAtColumn` 자체는 abstract 라 모든 base 상속 어댑터가 한 줄을 명시 override (실제 컬럼 또는 `null`) 해야 한다.
