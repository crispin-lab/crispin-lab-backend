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
    fun save(page: Page): Page
    fun findBy(id: PageId): Page?
    fun delete(id: PageId)
}
```

- 단건 조회는 `findBy(id)` — `OrNull` suffix 금지 (`conventions.md` "nullable 반환과 메서드명").
- `save` 는 신규/수정 모두 처리 (Exposed 의 `insertOrUpdate` 또는 `id` 존재 분기). 호출부에서 신경 안 쓰게.
  - **race 주의**: SELECT → INSERT 분기 패턴은 같은 ID 의 동시 호출이 들어오면 unique constraint 위반으로 깨진다. snowflake ID 단건 PK 는 충돌 확률이 매우 낮지만, slug 같은 unique 컬럼 기반 분기에는 동일 패턴을 복제하지 말고 `upsert` 로 한 번에 처리한다.
- 삭제는 `delete(id: PageId)`. soft delete 가 필요하면 `archive` 같은 도메인 메서드로 entity 상태를 바꾸고 `save` 하는 방식이 자연스럽다 — port 시그니처에 `softDelete` 류는 두지 않는다.

## Exposed 테이블 객체

```kotlin
package com.crispinlab.space.adapter.persistence.page

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Pages : Table("pages") {
    val id = long("id")
    val authorId = long("author_id")
    val title = varchar("title", length = 200)
    val body = text("body")
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

    public override fun delete(id: PageId): Unit = super.delete(id)

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

    protected abstract fun ResultRow.toEntity(): E
    protected abstract fun insert(entity: E)
    protected abstract fun update(entity: E)

    fun save(entity: E): E = ...                       // public — port 가 모두 노출
    fun findBy(id: I): E? = ...                        // public — port 가 모두 노출
    protected open fun findAllBy(ids: List<I>): List<E> = ...
    protected open fun delete(id: I) = ...
}
```

- 어댑터는 `table`, `idColumn`, `ResultRow.toEntity()`, `insert`, `update` 만 구현. SELECT → insert/update 분기와 findBy/findAllBy/delete 의 SQL 보일러플레이트는 base 가 통합.
- **노출 범위**: `save` / `findBy` 는 모든 어댑터의 port 가 노출하므로 base 에서 public. `findAllBy` / `delete` 는 `protected open` 으로 두고, port 시그니처가 정의된 어댑터에서만 `public override fun delete(id: I): Unit = super.delete(id)` 로 명시 expose. aggregate 내부 entity (예: `PageRevision`) 의 port 가 `delete` 를 정의하지 않으면 base 의 protected 가 그대로 유지되어 외부에서 호출 불가 — aggregate 일관성이 컴파일러로 보존된다.
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
