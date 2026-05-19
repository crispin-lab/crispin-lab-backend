package com.crispinlab.space.adapter.persistence

import com.crispinlab.common.domain.Entity
import com.crispinlab.common.domain.EntityId
import java.time.Instant
import java.time.Instant.now
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert

/**
 * Exposed 어댑터 공통 동작을 제공하는 abstract base.
 *
 * `save` / `findBy` 는 모든 어댑터의 port 가 노출하므로 public,
 * `findAllBy` / `delete` 는 protected 로 두어 port 시그니처가 정의된 어댑터에서만
 * visibility widening 으로 재노출한다 (aggregate 일관성 보존 — 예: `PageRevision` 은
 * `Page` aggregate 내부라 port 에 `delete` 가 없고 외부에서 호출되면 안 된다).
 *
 * Soft delete 지원: `deletedAtColumn` 을 SoftDeletable entity 어댑터는 실제 컬럼으로, hard delete 어댑터는 `null`
 * 로 명시 override. SoftDeletable 어댑터에선 `delete(id)` 가 hard DELETE 대신 `UPDATE deleted_at = now()` 로
 * 동작하고 `findBy` / `findAllBy` / 자식 도메인 특화 쿼리는 `notDeleted()` 헬퍼로 `deleted_at IS NULL` 자동 필터.
 * 정책 룰은 `repository.md` 참조.
 */
abstract class ExposedEntityRepository<E : Entity<I>, I : EntityId> {
    protected abstract val table: Table
    protected abstract val idColumn: Column<Long>

    /** SoftDeletable entity 의 어댑터는 실제 컬럼, hard delete 어댑터는 `null` 을 명시 override (`repository.md`). */
    protected abstract val deletedAtColumn: Column<Instant?>?

    protected abstract fun ResultRow.toEntity(): E

    /**
     * INSERT 와 UPDATE 양쪽에 적용되는 컬럼 매핑.
     * UPDATE 시 INSERT 값으로 덮이지 말아야 하는 immutable 컬럼은 [updateExclude] 에 명시한다.
     */
    protected abstract fun upsertBody(
        builder: UpsertStatement<Long>,
        entity: E
    )

    /** UPDATE 시 INSERT 값으로 덮이지 말아야 하는 immutable 컬럼 (예: createdAt, FK). default 는 비어 있음. */
    protected open val updateExclude: List<Column<*>> = emptyList()

    /**
     * 단일 SQL upsert (`INSERT ... ON CONFLICT (id) DO UPDATE SET ...`) 로 동작.
     * snowflake ID PK 든 unique 컬럼 분기든 race 없이 원자적.
     */
    fun save(entity: E): E {
        table.upsert(
            idColumn,
            onUpdateExclude = updateExclude
        ) { stmt ->
            upsertBody(stmt, entity)
        }
        return entity
    }

    fun findBy(id: I): E? =
        table
            .selectAll()
            .where { (idColumn eq id.value) and notDeleted() }
            .firstOrNull()
            ?.toEntity()

    protected open fun findAllBy(ids: List<I>): List<E> =
        if (ids.isEmpty()) {
            emptyList()
        } else {
            table
                .selectAll()
                .where { (idColumn inList ids.map { it.value }) and notDeleted() }
                .map { it.toEntity() }
        }

    protected open fun delete(id: I) {
        val column: Column<Instant?>? = deletedAtColumn
        if (column == null) {
            table.deleteWhere { idColumn eq id.value }
        } else {
            table.update({ (idColumn eq id.value) and column.isNull() }) {
                it[column] = now()
            }
        }
    }

    /**
     * `deletedAtColumn` 이 `null` 이면 (hard delete 어댑터) 항상 true, 컬럼이면 `deleted_at IS NULL`.
     * 자식 어댑터의 도메인 특화 쿼리에서도 같은 헬퍼로 일관 필터.
     */
    protected fun notDeleted(): Op<Boolean> = deletedAtColumn?.isNull() ?: Op.TRUE
}
