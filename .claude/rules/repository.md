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
- `save` 는 신규/수정 모두 처리. base 가 단일 SQL `INSERT ... ON CONFLICT (id) DO UPDATE SET ...` (Exposed v1 의 `org.jetbrains.exposed.v1.jdbc.upsert`) 로 원자 보장. 호출부에서 신경 안 쓰게.
  - **race 안전 (PK 한정)**: PK (`idColumn`) 충돌만 ON CONFLICT 로 처리. 동일 ID 의 동시 save 가 들어와도 unique constraint 위반 없이 한 row 로 수렴. **다른 unique index 의 race** (예: `Tags.(spaceId, name)`, `PageRevisions.(pageId, version)`) 는 그대로 `unique_violation` SQLException 으로 전파된다 — UseCase 레벨 사전 체크 (`existsByXxx`) + DB unique constraint 의 fail-fast 조합으로 보호하거나, 그 unique 컬럼을 `keys` 로 바꿔 별도 upsert 시그니처를 도입한다. 사용자 응답을 409 로 정정해야 하는 경우 (`ExposedTagRepository.save` 가 예) 어댑터에서 base 의 `save` 를 override 해 `ExposedSQLException.sqlState == "23505"` 만 잡아 `ConflictException` 으로 변환한다 — 그래서 base 의 `save` 는 `open` 이다. SQLState 판정 / 변환은 어댑터 책임 (UseCase 에 SQL 디테일 노출 금지).
  - **immutable 컬럼 보호**: UPDATE 시 INSERT 값으로 덮이면 안 되는 컬럼 (`id` PK, `createdAt`, FK 등) 은 어댑터의 `updateExclude` 리스트에 명시한다 — base 가 `onUpdateExclude` 로 SQL 의 SET 절에서 제외. `Page` 의 `authorId`/`spaceId`, `Comment` 의 `pageId`/`authorId` 등이 해당.
  - **반환값 시맨틱**: `save(entity)` 는 입력 entity 를 그대로 돌려준다 (DB row 를 다시 읽지 않는다). caller 가 `updateExclude` 로 보호되는 immutable 컬럼을 메모리에서 다른 값으로 들고 와 save 해도 반환값은 입력 entity 의 (사실과 다른) 메모리 상태를 그대로 노출하므로, caller 는 자기가 만든 entity 만 사용한다는 가정으로 호출한다.
  - **non-PK unique upsert 는 `Unit` 반환 + base 미상속**: (userId, spaceId) 같은 **PK 아닌 pair-unique** 로 upsert 해야 하는 aggregate (예: `SpaceVisit`) 는 `ExposedEntityRepository` base 를 상속하지 않는다 — base 의 `save` 가 `idColumn` 하나만 conflict target 으로 넘겨 `ON CONFLICT (id)` 을 생성하기 때문. 대신 어댑터에서 `SpaceVisits.upsert(SpaceVisits.userId, SpaceVisits.spaceId, onUpdateExclude = listOf(id, userId, spaceId)) { ... }` 형태의 DSL 을 직접 사용. 이 때 port `save` 시그니처는 반드시 **`Unit`** 으로 좁힌다 — conflict 발생 시 caller 가 넘긴 `id` 는 저장되지 않고 DB 의 기존 row `id` 가 유지되어 반환된 entity 의 id 를 신뢰하면 후속 lookup 이 miss 나는 함정이 생긴다. `Unit` 반환으로 이 함정을 시그니처 레벨에서 차단.
- 삭제는 `delete(id: PageId)`. port 시그니처는 항상 `delete(id)` 한 종류 — `softDelete` / `hardDelete` 류 분리 시그니처 금지.
  - **자동 분기**: entity 가 `SoftDeletable` (`lab-common-domain`) 을 implement 하고 어댑터가 `deletedAtColumn` 을 override 하면 base 의 `delete(id)` 가 `UPDATE deleted_at = now()` 로 동작. 같은 entity 의 `findBy` / `findAllBy` 와 도메인 특화 쿼리는 `notDeleted()` 헬퍼로 `deleted_at IS NULL` 자동 필터.
  - **hard delete 유지**: `deletedAtColumn` 미 override 면 기존 SQL DELETE. Tag / PageRevision / PageLink / PageTag 처럼 종속·association entity 는 hard delete 유지. `Page` 가 soft delete 돼도 `page_tags` 매핑 row 는 보존된다 (`page_tags.page_id` FK 의 CASCADE 가 hard DELETE 가 아닌 한 트리거되지 않음) — page 가 복구되면 tag 매핑도 자연 복원. 단 tag 기반 검색 (`PageSearchPort` 등) 은 join 시 `pages.deleted_at IS NULL` 을 명시 필터해 deleted page 를 노출하지 않는다.
  - **표준 흐름은 `repository.delete(id)` 한 줄**: UseCase 는 `findBy + takeIf` 로 권한/소유자 검증 후 `repository.delete(id)` 호출. 이미 deleted 인 entity 는 `findBy` 자동 필터로 못 찾으니 NotFoundException 으로 fallback — invariant 가 자연스럽게 보호된다 (이중 삭제 시도 차단). `Page`, `Space`, `Comment` 모두 이 표준을 따른다.
  - **`deletedAt` 은 `updateExclude` 에 포함**: SoftDeletable 어댑터의 `deletedAt` 컬럼은 `repository.delete(id)` 만이 갱신해야 하므로 `updateExclude` 에 명시해 `save` 가 절대 건드리지 못하게 한다. silent undelete 방지.

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
import com.crispinlab.user.domain.user.UserId
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
    override val deletedAtColumn = Pages.deletedAt
    override val updateExclude =
        listOf(Pages.id, Pages.spaceId, Pages.authorId, Pages.createdAt, Pages.deletedAt)

    override fun ResultRow.toEntity(): Page =
        Page(
            id = PageId(this[Pages.id]),
            authorId = UserId(this[Pages.authorId]),
            title = this[Pages.title],
            content = PageContent(this[Pages.content]),
            visibility = decodeVisibility(this[Pages.visibility]),
            createdAt = this[Pages.createdAt],
            updatedAt = this[Pages.updatedAt],
            deletedAt = this[Pages.deletedAt]
        )

    @Suppress("RedundantOverride")
    override fun delete(id: PageId) = super.delete(id)

    override fun upsertBody(builder: UpsertStatement<Long>, entity: Page) {
        builder[Pages.id] = entity.id.value
        builder[Pages.authorId] = entity.authorId.value
        builder[Pages.title] = entity.title
        builder[Pages.content] = entity.content.raw
        builder[Pages.visibility] = entity.visibility.name
        builder[Pages.createdAt] = entity.createdAt
        builder[Pages.updatedAt] = entity.updatedAt
        builder[Pages.deletedAt] = entity.deletedAt
    }

    private fun decodeVisibility(stored: String): Visibility =
        runCatching { stored.asVisibility() }
            .getOrElse { cause ->
                throw IllegalStateException("저장된 visibility 값을 해석할 수 없습니다.", cause)
            }
}
```

### ExposedEntityRepository base

`lab-common-persistence/src/main/kotlin/com/crispinlab/common/persistence/ExposedEntityRepository.kt` 에 다음 abstract class 가 있다 (`com.crispinlab.common.persistence` 패키지):

`E` 는 `Entity<I>` 마커를 만족해야 한다 (`entity.md` 참조). base 의 시그니처:

```kotlin
abstract class ExposedEntityRepository<E : Entity<I>, I : EntityId> {
    protected abstract val table: Table
    protected abstract val idColumn: Column<Long>
    protected abstract val deletedAtColumn: Column<Instant?>?     // SoftDeletable 어댑터는 실제 컬럼, 그 외는 null 명시 override

    protected abstract fun ResultRow.toEntity(): E
    protected abstract fun upsertBody(builder: UpsertStatement<Long>, entity: E)
    protected open val updateExclude: List<Column<*>> = emptyList()

    open fun save(entity: E): E = ...                  // public — port 가 모두 노출. 단일 SQL upsert (INSERT ... ON CONFLICT (id) DO UPDATE SET ...). updateExclude 컬럼은 SET 절에서 제외. SQLState 변환이 필요한 어댑터는 override
    fun findBy(id: I): E? = ...                        // public — port 가 모두 노출. deletedAtColumn 이 컬럼이면 자동 필터
    protected open fun findAllBy(ids: List<I>): List<E> = ...
    protected open fun delete(id: I) = ...             // deletedAtColumn 이 컬럼이면 UPDATE, null 이면 DELETE

    protected fun notDeleted(): Op<Boolean> = deletedAtColumn?.isNull() ?: Op.TRUE
}
```

- 어댑터는 `table`, `idColumn`, `deletedAtColumn`, `ResultRow.toEntity()`, `upsertBody`, (필요 시) `updateExclude` 를 구현. 단일 SQL upsert / findBy / findAllBy / delete 의 SQL 보일러플레이트는 base 가 통합. **`deletedAtColumn` 은 abstract** — SoftDeletable entity 어댑터는 실제 컬럼 (`Pages.deletedAt`) 을, hard delete 어댑터는 `null` 을 명시 override. `open val` + default null 로 두면 base 안에서의 nullability 추론이 IDE 에서 false positive (`Useless null check` / `Useless null-safe access`) 를 띄우므로 abstract 가 정합.
- **`upsertBody` 단일 매핑**: INSERT 와 UPDATE 양쪽에 적용되는 컬럼 매핑이 한 함수로 통합. INSERT 만 채우고 UPDATE 에선 제외해야 하는 immutable 컬럼은 `updateExclude` 리스트에 명시 — base 가 `Table.upsert(..., onUpdateExclude = updateExclude) { ... }` 로 SQL 의 SET 절에서 제외한다. `updateExclude` 표준 구성: `idColumn`, `createdAt`, FK 컬럼, SoftDeletable 어댑터의 `deletedAtColumn`. PageRevision 처럼 insert-only entity 도 최소한 `id` 와 `createdAt` 은 명시한다 — 일관성 차원의 immutable 표명. 단 모든 컬럼을 `updateExclude` 에 두면 SET 절이 빈 SQL 이 만들어져 syntax error 가 나니 적어도 한 컬럼은 update 대상으로 남긴다.
- **노출 범위**: `save` / `findBy` 는 모든 어댑터의 port 가 노출하므로 base 에서 public. `findAllBy` / `delete` 는 `protected open` 으로 두고, port 시그니처가 정의된 어댑터에서만 `@Suppress("RedundantOverride") override fun delete(id: I) = super.delete(id)` 로 명시 expose.

### delete expose 의 두 inspection 함정

자식 어댑터가 두 부모 (`ExposedEntityRepository` 의 `protected delete` + port interface 의 `public delete`) 를 동시에 override 하는 케이스라 IDE inspection 이 두 가지 false positive 를 띄운다 — 둘 다 무시하면 안 되는 의도된 패턴:

1. **`public override` 의 `public` 은 redundant** — Kotlin 의 visibility override 규칙상 두 부모 중 가장 넓은 visibility (public) 가 자동 적용되므로 `public` modifier 가 진짜 redundant. 이건 따라서 제거 (`override` 만 남김).
2. **`override` 자체는 redundant 가 아님** — body 가 `super.delete(id)` 한 줄뿐이라 IntelliJ 가 "Redundant overriding method" inspection 으로 잡지만, 이걸 따라 제거하면 `Cannot weaken access privilege` 컴파일 에러가 난다 (base 의 protected 가 interface 의 public 보다 좁아 자동 inherit 로는 interface 시그니처를 만족 못 시킴). 즉 override 의 **유일한 역할이 visibility widening (protected → public)** 인데 IDE inspection 이 body 만 보고 그 효과를 인식 못 함 (KT-46667 류). `@Suppress("RedundantOverride")` 로 명시 우회 — `conventions.md` "IDE 경고는 무시하지 않는다. 무시해야 하면 `@Suppress` 로 명시" 정합. 이유는 본 룰 문서로 한 곳에 모으고 코드 측 주석은 두지 않는다.

### 왜 base 가 protected 인가

base 가 처음부터 public 이면 `PageRevisionRepository` 처럼 port 에 `delete` 가 없는 어댑터에서도 외부 호출이 가능해져 aggregate 일관성이 깨진다. base 를 protected 로 두면 port 가 명시한 어댑터만 visibility widening 으로 expose 하므로, aggregate 경계가 컴파일러로 보존된다. admin / 복구 UseCase 같은 새 책임이 필요해지면 같은 패턴 — base 의 protected helper + port 추가 + 자식 expose 를 그 PR 에서 함께 도입 (`conventions.md` "구현 없는 포트/미사용 코드" 정합).
- **자동 필터 헬퍼 `notDeleted()`**: `deletedAtColumn` 이 `null` 이면 `Op.TRUE` 로 풀려 hard delete 어댑터에서도 동일 호출 가능. 자식 어댑터의 도메인 특화 쿼리에서 `where { (Pages.parentPageId eq parentId.value) and notDeleted() }` 같이 한 줄 추가로 일관 필터. 새 도메인 쿼리 작성 시 누락하면 deleted row 가 노출 — PR 체크리스트 항목. base 를 상속하지 않는 별도 어댑터 (예: `ExposedPageSearchAdapter`) 도 동일 정신으로 `Pages.deletedAt.isNull()` 을 직접 명시.
- **silent undelete 차단은 `updateExclude` 로**: SoftDeletable 어댑터의 `deletedAtColumn` 을 `updateExclude` 에 포함하면 base 의 upsert SQL 의 SET 절에서 자동 제외되어, `save` 가 임의의 entity 의 `deletedAt = null` 을 받아도 deleted row 의 `deleted_at` 을 덮지 못한다. 표준 삭제 흐름 (`repository.delete(id)`) 만이 `deleted_at` 을 갱신한다.
- 어댑터 클래스명 prefix 는 **기술 스택**(`Exposed`) 으로. `MySql`, `Redis` 등도 같은 결.
- 도메인 특화 메서드 (`findByPageId`, `findBySpaceId`, `attach/detach` 등) 는 어댑터에 그대로 둔다 — base 가 일반화하지 않는다.
- **invariant 직렬화는 어댑터 안에서 `.forUpdate()` 로 격하**: 도메인 invariant 가 *count + 후속 write* 같이 read-then-write 흐름에 걸쳐 있을 때 (예: `SpaceMember` 의 "마지막 OWNER 차단"), application 사전 체크만으로는 READ_COMMITTED 에서 race window 가 열린다. 해결은 어댑터의 SELECT 에 `.forUpdate()` 한 줄 — 호출 측 (UseCase) 의 시그니처를 단순 read (`countOwnersBy(spaceId): Long`) 로 유지하면서 같은 트랜잭션의 후속 `delete`/`save` 가 commit 까지 자연 직렬화된다. lock 의도는 port 시그니처에 누설하지 않고 어댑터의 race 안전 메커니즘으로 남는다 (`ExposedSpaceMemberRepository.countOwnersBy` 가 예). SERIALIZABLE 격상은 retry infra 가 필요해 본 도메인에서는 과도.
  - **호출 측 제약**: port 시그니처가 단순 read 처럼 보여도 호출은 반드시 `transactional { ... }` 안에서 — 트랜잭션 밖에서 호출하면 lock 자체가 의미 없음. 룰 어기면 race 보호가 silently 풀린다.
  - **lock-set 한계**: `.forUpdate()` 는 결과 row 의 row lock (predicate / gap lock 아님). `WHERE role = 'OWNER'` 의 *기존* OWNER row 만 잡으므로, 다른 트랜잭션이 같은 Space 에 새 OWNER 를 INSERT 하는 race 는 직렬화되지 않는다. 본 도메인의 "마지막 OWNER 차단" invariant 는 *OWNER count 가 0 으로 떨어지지 않는다* 한 가지라 INSERT 가 늘리는 방향이라 자연 안전. 미래에 invariant 가 "OWNER 가 정확히 N 명" 같이 양방향으로 바뀌면 predicate lock / advisory lock (`pg_advisory_xact_lock`) 별도로.
  - **row 수 제약**: `select(id).forUpdate().toList().size` 패턴은 lock 잡힐 row 가 작을 때 (OWNER 수십 명 단위) 만 안전. 수천 row 를 lock-then-count 하면 ResultSet materialize + lock 비용이 폭주. row 가 커지는 도메인이 등장하면 별도 SQL `COUNT(*)` + 명시적 advisory lock 으로 분리.
- 도메인 port Repository 인터페이스 (`PageRepository` 등) 는 **공통 super type 없이** 각자 정의. base 가 강제하는 추상화는 어댑터 측에만.
- `saveAll` / `batchInsert` 만 쓰는 어댑터 (예: `ExposedPageLinkRepository`) 는 base 의 단건 CRUD 가 무의미하므로 base 상속하지 않는다.

### 작성 시 따져볼 것

- 매핑 함수 이름은 `ResultRow.toEntity()` — `from`, `mapToPage` 등 흩뿌리지 말 것. 한 어댑터 안에서 일관.
- enum 은 컬럼에 `name` 으로 저장하고 읽을 때 `asXxx()` 로 복원. 인덱스가 필요하면 별도 정수 컬럼 고려.
- **DB 손상 enum 매핑은 `IllegalStateException` 으로 래핑** — `asVisibility()` 가 `IllegalArgumentException` 을 던지지만, 어댑터에서 그대로 흘리면 `GlobalExceptionHandler` 가 400 으로 매핑한다. DB 에 깨진 값이 들어 있는 건 외부 입력 오류가 아니라 운영 결함이므로 `decodeXxx` 헬퍼로 래핑해 500 으로 응답되게 한다 (`error-messages.md` 의 "외부 입력 / 내부 상태" 구분과 정합).
- **batch projection 어댑터 (`Query`/`*HandleQuery` 류) 의 corrupt row 는 fail-fast 대신 skip + WARN** — 단건 매핑 (`findBy` → `toEntity`) 은 한 row 가 깨지면 그 한 요청이 실패해도 무해하지만, batch lookup 은 한 row 의 손상이 정상 N-1 row 의 응답까지 막아 가용성 사고로 증폭된다. 따라서 batch 어댑터의 `decodeXxx` 는 `Handle?` / null 반환 + `mapNotNull` 로 누락 + `LoggerFactory.getLogger(javaClass)` 의 `warn` 로그 한 줄로 운영 알림 (`logger 이름은 클래스 기반`, `logging.md` 룰 1 의 `application/domain` 금지 범위 밖 — 어댑터는 허용). 호출 측 (UseCase) 의 "lookup miss → 빈 문자열" sentinel 정책과 자연 결합되어 시그니처 변경 없이 일관 처리. WARN 메시지엔 식별자 (`userId.value` 등) 만 노출, 손상된 raw 값은 노출하지 않는다 (PII / 노이즈 회피). 예: `ExposedUserHandleQueryAdapter.decodeHandle`.
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

### SQL 검색 어댑터 정책

`ExposedPageSearchAdapter` 같은 SQL 기반 검색 어댑터의 두 invariant — ES 어댑터 교체 전까지 유효.

**visibility 룰 재사용은 `VisibilityScope.toClauses()` + 두 직렬화기**: `lab-space/app/.../adapter/persistence/visibility/` 모듈이 SOP (Sum-of-Products) 형태의 `VisibilityClause` 리스트 하나를 산출하고 `toExposedOp()` (DSL `Op<Boolean>`) / `toSqlFragment()` (raw SQL + args) 두 직렬화기로 나눠 소비된다. 새 검색 entrypoint 는 어댑터 안에서 visibility 정책을 직접 인코딩하지 않고 이 모듈만 호출 — DSL 어댑터면 `scope.toClauses().toExposedOp()`, raw SQL 어댑터면 `scope.toClauses().toSqlFragment()`. 룰 변경은 `toClauses()` 한 함수에서만 발생해 두 직렬화기가 자동 동기화. `toClauses()` 는 세 브랜치 모두 최소 1 clause 를 반환하는 invariant — 두 직렬화기의 `check(isNotEmpty())` 는 이 invariant 를 fail-fast 로 지키는 방어책 (empty list 는 도달하지 않지만 새 브랜치 추가 시 의미 결정을 강제).

**case-insensitive 매칭은 `LOWER() + LIKE`**: Postgres ILIKE 직접 사용 대신 `Column.lowerCase() like %lower(keyword)%` 조합 (Exposed v1 core 의 `lowerCase()`). ANSI 표준 LIKE 만 사용해 vendor portable, `escapeLike()` (`%` / `_` / `\` → `\` prefix) 가 그대로 유효. Exposed v1 의 `like` infix 는 `String` 인자를 `LikePattern(pattern, escapeChar = null)` 로 wrap 하므로 `ESCAPE` 절을 SQL 에 명시하지 않는다 — Postgres LIKE 의 표준 default escape 가 `\` 이고 `standard_conforming_strings = on` (PG 9.1+ default) 가정에서 안전. column 쪽 SQL `LOWER` 와 pattern 쪽 Kotlin `lowercase()` 의 locale 차이는 ASCII / 한국어 한정 무해 — 비-ASCII case-insensitive 가 요구되면 ICU locale 정합을 별도로.

**정렬 옵션 — RELEVANCE 는 임시로 UPDATED_AT 컬럼 fallback**: `PageSearchPort.SortOption` 의 `CREATED_AT` / `UPDATED_AT` 은 실제 컬럼 정렬, `RELEVANCE` 는 임시로 `UPDATED_AT` 과 동일 컬럼 (`Pages.updatedAt DESC, Pages.id DESC`) 으로 fallback. SQL `LOWER + LIKE` 는 진짜 ranking 을 제공하지 않으므로 별도 분기를 두지 않고 묶는다. ES 어댑터 교체 시점 (BM25 / 벡터 ranking 도입) 에 분기를 분리. 어댑터의 `when` 분기에서 `UPDATED_AT, RELEVANCE -> { ... }` 한 케이스로 묶여 있는 것은 이 정책의 표현이다 — 코드 옆에 별도 주석을 두지 않고 본 룰 문서가 의도를 운반한다.

**태그 AND 매칭은 두 단계 분리 쿼리**: 다중 tagIds 의 AND 보장 (`HAVING COUNT(DISTINCT tag_id) = N`) 은 별도 `matchedPageIdsByTag` 쿼리로 page_id 리스트를 먼저 모은 뒤 baseQuery 의 `Pages.id inList tagPageIds` 로 합성. 단일 쿼리에 group by + having + 일반 where 를 통합하지 않는 이유: baseQuery 의 `count()` (`toPageResult` 내부) 가 join row 곱셈으로 부풀려지는 회귀 방지. round-trip 1회 추가는 의도된 비용.

**`tagIdsAnyOf` 는 OR sub-query (HAVING 없음)**: 한 그룹 안에서 OR 매칭이 필요한 경우의 별도 파라미터. `matchedPageIdsByAnyTag` 가 `HAVING COUNT(...) = N` 없이 `GROUP BY page_id` 만 두어 OR 시맨틱을 표현, 같은 baseQuery 에 `Pages.id inList anyOfPageIds` 한 절을 추가. `tagIds` (AND) 와 함께 오면 두 sub-query 의 결과가 baseQuery 안에서 자연 AND 결합. AND 와 OR sub-query 가 SQL 본문은 동일하고 `having` 절만 분기하므로 어댑터 안에서는 `matchedPageIdsBy(tagIds, requireAllMatch)` 한 함수로 묶고 `matchedPageIdsByTag` / `matchedPageIdsByAnyTag` 가 thin wrapper 로 호출 측 의도를 유지 — soft-delete / visibility 필터를 두 곳에 중복 인코딩하지 않아 한쪽만 갱신되는 회귀를 차단.

**`tagName → tagIds` cross-space 해석은 UseCase 책임 (`TagRepository.findIdsByName`)**: 어댑터는 OR 시맨틱만 알고 name 추상은 모른다. tagName lookup 이 0건이면 UseCase 에서 short-circuit (port 호출 없이 `PageResult.empty`) — 어댑터의 `tagIdsAnyOf` 는 기존 `tagIds` 와 동일하게 `emptyList() = no constraint` 시맨틱. cross-space name lookup 의 hot path 보호를 위해 `tags(name)` 단독 인덱스 (`tags_name_idx`) 가 별도로 들어가 있다 (landing TagCloud chip → `/v1/pages?tagName=foo` 가 빈번 클릭 경로).

**Exposed DSL 로 표현 곤란한 SQL 은 raw SQL 로 격하 허용**: subquery alias + `COALESCE`, `DISTINCT ON`, window function 같이 Exposed v1 DSL 로 표현이 부담스러운 SQL 이 검색·조회 어댑터에 필요하면 `TransactionManager.current().exec(stmt, args, StatementType.SELECT) { rs -> ... }` 로 raw SQL 을 실행한다. 조건: (1) **same-transaction 참여** — 어댑터가 새 트랜잭션을 열지 않고 UseCase 의 `transactional { }` 안에서 흐른다 가정, (2) **argument 는 반드시 `IColumnType` + `?` 바인딩** (SQL injection 차단), (3) **enum name 등 도메인 리터럴만 SQL 문자열에 직접 삽입** (예: `direction.name` = `ASC`/`DESC`), (4) **soft delete 필터 (`WHERE deleted_at IS NULL`) 명시 책임은 어댑터** — base 의 `notDeleted()` 자동 필터가 raw SQL 에 적용되지 않는다, (5) **timestamp 는 `rs.getTimestamp(col).toInstant()`** — `getObject(col, LocalDateTime::class).toInstant(ZoneOffset.UTC)` 는 JVM 타임존이 UTC 가 아니면 offset 오차가 발생 (LAB-182 회귀 사례). 예: `ExposedSpaceRepository.findPage` (LEFT JOIN + MAX subquery + COALESCE), `ExposedPageSearchAdapter.latestsBySpaceIds` (DISTINCT ON), `ExposedPageAncestorAdapter` (WITH RECURSIVE — 아래 절 참조).

### 트리 traversal 어댑터 정책

`ExposedPageAncestorAdapter` 처럼 Postgres `WITH RECURSIVE` CTE 로 부모/자식 chain 을 따라가는 그래프 조회 어댑터의 invariant. Exposed v1 DSL 이 recursive CTE 를 표현하지 못해 본 어댑터들은 `TransactionManager.current().exec(stmt, args, StatementType.SELECT) { rs -> ... }` 로 raw SQL 을 실행한다 — 본 저장소의 첫 raw SQL 사용 케이스. 트랜잭션은 UseCase 의 `transactional` 안에서 흐른다 가정 (어댑터는 새 트랜잭션을 열지 않는다).

**Cross-{aggregate scope} 차단은 SQL 레벨**: page chain 이 `parent_page_id` 만 따라가면 다른 스페이스의 page 가 끼어도 끌려온다. FK 가 없어 (`migration.md`) application 정책으로 보장된 invariant 라도 데이터 잔재·관리 콘솔 변경으로 깨질 수 있다. CTE anchor 는 `INNER JOIN pages target ON target.id = ? AND ... AND p.space_id = target.space_id` 로 target 의 space_id 와 같은 스페이스만 select, 재귀 절은 `INNER JOIN ancestor_chain ac ON ... AND p.space_id = ac.space_id` 로 chain 유지. visibility 마스킹 (UseCase 의 `scope.allows()`) 은 마지막 방어선이고, 첫 방어선은 SQL 의 same-scope 강제. cross-space invariant 가 깨질 가능성이 있는 다른 그래프 traversal (자식, 자손, 인접 등) 도 같은 패턴.

**Soft delete 는 CTE 의 anchor + recursive 양쪽에 명시**: `WHERE p.deleted_at IS NULL` 을 둘 다 둔다. anchor 에만 두면 chain 중간의 deleted row 도 끌려와 응답에 노출. 양쪽에 두면 deleted row 에서 recursion 이 자연 종료 — 의도된 동작 ("deleted page 는 도메인적으로 존재하지 않음"). 본 어댑터의 base 자동 필터 (`notDeleted()`) 는 raw SQL 에 적용되지 않으므로 SQL 안에 명시 책임은 어댑터.

**깊이 가드는 const 로**: 무한 순환 / 운영 사고 방지용 상한. `ExposedPageAncestorAdapter` 는 `MAX_ANCESTORS = 64`. 의미는 "본 시스템은 64 단계 이상의 페이지 chain 을 가정하지 않는다"  — 초과 시 silent truncation (root 가 응답에서 빠짐). 위키 실무 깊이를 충분히 덮는 보수적 값이라 트리거·모니터링 분리는 미도입. 64 단계가 부족해지는 정황이 보이면 응답에 truncation flag 를 추가하거나 가드 값을 늘리는 정책 결정이 필요 (별도 티켓). 다른 그래프 어댑터를 추가할 때도 같은 정책 — `const val MAX_<traversal> = N` 으로 한 곳에 모은다.

## 자주 빠뜨리는 것

- **port 가 `lab-{domain}/domain` 에 들어감** — port 는 `application` 패키지. domain 은 entity / value object 만.
- **Repository 가 도메인 메서드를 호출** — `save` 안에서 `page.publish()` 같은 도메인 동작 호출 금지. 그건 UseCase 책임.
- **`findById` 라는 이름** — `findBy(id: PageId)` 가 본 저장소 컨벤션. 시그니처가 ID 임을 이미 보여준다.
- **컬럼 누락 매핑** — `upsertBody` 와 `toEntity()` 가 비대칭이면 사일런트 데이터 손실. PR 체크리스트 (필드 추가) 항목으로 묶어 확인.
- **Exposed `transaction { ... }` 을 어댑터 안에서** — 트랜잭션 경계는 UseCase 단계가 가져간다 (`usecase-implementation.md` "트랜잭션 경계"). 어댑터는 현재 트랜잭션을 가정.
- **테이블 객체명 - 테이블명 불일치** — `object PageTable : Table("pages")` 같은 형태. `object Pages : Table("pages")` 로.
- **SoftDeletable 어댑터의 도메인 특화 쿼리에 `notDeleted()` 누락** — base 의 `findBy`/`findAllBy` 만 자동 필터되고, `findChildren`/`findRoots`/`findByPageId` 같은 자식 쿼리는 손수 `and notDeleted()` 를 붙여야 한다. 누락 시 deleted row 가 노출. hard delete 어댑터에서도 호출 가능 (`Op.TRUE` 로 풀림) 하니 패턴 일관성 차원에서 추가해도 무해.
- **`deletedAtColumn` 을 실제 컬럼으로 override 했는데 entity 가 `SoftDeletable` implement 안 함 (또는 반대)** — `toEntity()`/`upsertBody()` 에서 `entity.deletedAt` 접근 시 컴파일 에러로 즉시 잡힘. 한쪽만 추가하면 build 가 잡으므로 동시 변경 강제. `deletedAtColumn` 자체는 abstract 라 모든 base 상속 어댑터가 한 줄을 명시 override (실제 컬럼 또는 `null`) 해야 한다.
- **SoftDeletable 어댑터에서 `deletedAtColumn` 을 `updateExclude` 에 누락** — `save` 가 entity 의 `deletedAt = null` 을 덮어쓰면 silent undelete 가 발생. 표준 삭제 흐름이 `repository.delete(id)` 한 줄이라 통상 발생은 안 하지만, 패턴 일관성 + 사고 방지로 `updateExclude` 에 항상 포함.
