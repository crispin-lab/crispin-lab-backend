package com.crispinlab.space.adapter.persistence

import com.crispinlab.common.domain.Entity
import com.crispinlab.common.domain.EntityId
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Exposed 어댑터 공통 동작을 제공하는 abstract base.
 *
 * `save` / `findBy` 는 모든 어댑터의 port 가 노출하므로 public,
 * `findAllBy` / `delete` 는 protected 로 두어 port 시그니처가 정의된 어댑터에서만
 * `override`/`public` 으로 재노출한다 (aggregate 일관성 보존 — 예: `PageRevision` 은
 * `Page` aggregate 내부라 port 에 `delete` 가 없고 외부에서 호출되면 안 된다).
 */
abstract class ExposedEntityRepository<E : Entity<I>, I : EntityId> {
    protected abstract val table: Table
    protected abstract val idColumn: Column<Long>

    protected abstract fun ResultRow.toEntity(): E

    protected abstract fun insert(entity: E)

    protected abstract fun update(entity: E)

    /**
     * SELECT → insert/update 분기.
     *
     * snowflake ID 단건 PK 가정이라 동시 insert 충돌 확률이 매우 낮다.
     * slug 같은 unique 컬럼 기반 분기에는 이 패턴을 복제하지 말고 `upsert` 로
     * 한 번에 처리한다 (`repository.md` "race 주의" 참조).
     */
    fun save(entity: E): E =
        table
            .selectAll()
            .where { idColumn eq entity.id.value }
            .firstOrNull()
            ?.let {
                update(entity)
                entity
            } ?: entity.also { insert(it) }

    fun findBy(id: I): E? =
        table
            .selectAll()
            .where { idColumn eq id.value }
            .firstOrNull()
            ?.toEntity()

    protected open fun findAllBy(ids: List<I>): List<E> =
        if (ids.isEmpty()) {
            emptyList()
        } else {
            table
                .selectAll()
                .where { idColumn inList ids.map { it.value } }
                .map { it.toEntity() }
        }

    protected open fun delete(id: I) {
        table.deleteWhere { idColumn eq id.value }
    }
}
