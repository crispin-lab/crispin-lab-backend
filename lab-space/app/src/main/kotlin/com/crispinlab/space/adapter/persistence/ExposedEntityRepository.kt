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

abstract class ExposedEntityRepository<E : Entity<I>, I : EntityId> {
    protected abstract val table: Table
    protected abstract val idColumn: Column<Long>

    protected abstract fun ResultRow.toEntity(): E

    protected abstract fun insert(entity: E)

    protected abstract fun update(entity: E)

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

    fun findAllBy(ids: List<I>): List<E> =
        if (ids.isEmpty()) {
            emptyList()
        } else {
            table
                .selectAll()
                .where { idColumn inList ids.map { it.value } }
                .map { it.toEntity() }
        }

    fun delete(id: I) {
        table.deleteWhere { idColumn eq id.value }
    }
}
