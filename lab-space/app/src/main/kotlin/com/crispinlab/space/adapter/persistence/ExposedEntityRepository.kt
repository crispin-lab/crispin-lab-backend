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

abstract class ExposedEntityRepository<E : Entity<I>, I : EntityId> {
    protected abstract val table: Table
    protected abstract val idColumn: Column<Long>
    protected abstract val deletedAtColumn: Column<Instant?>?

    protected abstract fun ResultRow.toEntity(): E

    protected abstract fun upsertBody(
        builder: UpsertStatement<Long>,
        entity: E
    )

    protected open val updateExclude: List<Column<*>> = emptyList()

    open fun save(entity: E): E {
        table.upsert(
            idColumn,
            onUpdateExclude = updateExclude
        ) { statement ->
            upsertBody(statement, entity)
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

    protected fun notDeleted(): Op<Boolean> = deletedAtColumn?.isNull() ?: Op.TRUE
}
