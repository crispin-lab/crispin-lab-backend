package com.crispinlab.space.adapter.persistence.space

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.persistence.ExposedEntityRepository
import com.crispinlab.common.persistence.escapeLike
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository.SortDirection
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository.SortOption
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository.Summary
import com.crispinlab.space.application.port.outgoing.space.SpaceVisibilityScope
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.space.SpaceVisibility.Companion.asSpaceVisibility
import java.time.Instant
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.springframework.stereotype.Repository

@Repository
class ExposedSpaceRepository :
    ExposedEntityRepository<Space, SpaceId>(),
    SpaceRepository {
    override val table = Spaces
    override val idColumn = Spaces.id
    override val deletedAtColumn = Spaces.deletedAt
    override val updateExclude = listOf(Spaces.id, Spaces.createdAt, Spaces.deletedAt)

    @Suppress("RedundantOverride")
    override fun delete(id: SpaceId) = super.delete(id)

    override fun findVisibility(id: SpaceId): SpaceVisibility? =
        Spaces
            .select(Spaces.visibility)
            .where { (Spaces.id eq id.value) and notDeleted() }
            .singleOrNull()
            ?.let { decodeSpaceVisibility(it[Spaces.visibility]) }

    override fun ResultRow.toEntity(): Space =
        Space(
            id = SpaceId(this[Spaces.id]),
            name = this[Spaces.name],
            description = this[Spaces.description],
            visibility = decodeSpaceVisibility(this[Spaces.visibility]),
            createdAt = this[Spaces.createdAt],
            updatedAt = this[Spaces.updatedAt],
            deletedAt = this[Spaces.deletedAt]
        )

    override fun upsertBody(
        builder: UpsertStatement<Long>,
        entity: Space
    ) {
        builder[Spaces.id] = entity.id.value
        builder[Spaces.name] = entity.name
        builder[Spaces.description] = entity.description
        builder[Spaces.visibility] = entity.visibility.name
        builder[Spaces.createdAt] = entity.createdAt
        builder[Spaces.updatedAt] = entity.updatedAt
        builder[Spaces.deletedAt] = entity.deletedAt
    }

    override fun findPage(
        pageRequest: PageRequest,
        scope: SpaceVisibilityScope,
        keyword: String?,
        sort: SortOption,
        direction: SortDirection
    ): PageResult<Summary> {
        val fragment = whereFragment(scope, keyword)
        val orderClause = orderClause(sort, direction)
        val totalElements = countBy(fragment)
        val items =
            if (totalElements == 0L) {
                emptyList()
            } else {
                selectPage(fragment, orderClause, pageRequest.size, pageRequest.offset)
            }
        return PageResult(
            items = items,
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = totalElements
        )
    }

    private fun countBy(fragment: SqlFragment): Long {
        val sql =
            """
            SELECT COUNT(*)
            FROM spaces s
            WHERE s.deleted_at IS NULL
              AND ${fragment.sql}
            """.trimIndent()
        var count = 0L
        TransactionManager.current().exec(
            stmt = sql,
            args = fragment.args,
            explicitStatementType = StatementType.SELECT
        ) { rs ->
            if (rs.next()) count = rs.getLong(1)
        }
        return count
    }

    private fun selectPage(
        fragment: SqlFragment,
        orderClause: String,
        limit: Int,
        offset: Long
    ): List<Summary> {
        val sql =
            """
            SELECT s.id, s.name, s.description, s.visibility, s.created_at, s.updated_at,
                   COALESCE(la.max_updated_at, s.updated_at) AS last_activity_at
            FROM spaces s
            LEFT JOIN (
                SELECT space_id, MAX(updated_at) AS max_updated_at
                FROM pages
                WHERE deleted_at IS NULL
                GROUP BY space_id
            ) la ON la.space_id = s.id
            WHERE s.deleted_at IS NULL
              AND ${fragment.sql}
            ORDER BY $orderClause
            LIMIT ? OFFSET ?
            """.trimIndent()
        val args =
            buildList<Pair<IColumnType<*>, Any?>> {
                addAll(fragment.args)
                add(LongColumnType() to limit.toLong())
                add(LongColumnType() to offset)
            }
        return buildList {
            TransactionManager.current().exec(
                stmt = sql,
                args = args,
                explicitStatementType = StatementType.SELECT
            ) { rs ->
                while (rs.next()) {
                    add(rs.toSummary())
                }
            }
        }
    }

    private fun java.sql.ResultSet.toSummary(): Summary =
        Summary(
            spaceId = SpaceId(getLong("id")),
            name = getString("name"),
            description = getString("description"),
            visibility = decodeSpaceVisibility(getString("visibility")),
            lastActivityAt = getInstant("last_activity_at"),
            createdAt = getInstant("created_at"),
            updatedAt = getInstant("updated_at")
        )

    private fun java.sql.ResultSet.getInstant(column: String): Instant =
        getTimestamp(column).toInstant()

    private fun whereFragment(
        scope: SpaceVisibilityScope,
        keyword: String?
    ): SqlFragment {
        val parts = mutableListOf<String>()
        val args = mutableListOf<Pair<IColumnType<*>, Any?>>()
        scope.appendTo(parts, args)
        keyword?.let {
            parts += "LOWER(s.name) LIKE ?"
            args += TextColumnType() to "%${it.lowercase().escapeLike()}%"
        }
        return SqlFragment(sql = parts.joinToString(" AND "), args = args)
    }

    private fun SpaceVisibilityScope.appendTo(
        parts: MutableList<String>,
        args: MutableList<Pair<IColumnType<*>, Any?>>
    ) {
        when (this) {
            is SpaceVisibilityScope.Anonymous -> {
                parts += "s.visibility = ?"
                args += TextColumnType() to SpaceVisibility.PUBLIC.name
            }

            is SpaceVisibilityScope.Authenticated -> {
                if (memberOfSpaceIds.isEmpty()) {
                    parts += "s.visibility = ?"
                    args += TextColumnType() to SpaceVisibility.PUBLIC.name
                } else {
                    val placeholders = memberOfSpaceIds.joinToString(", ") { "?" }
                    parts += "(s.visibility = ? OR (s.visibility = ? AND s.id IN ($placeholders)))"
                    args += TextColumnType() to SpaceVisibility.PUBLIC.name
                    args += TextColumnType() to SpaceVisibility.INTERNAL.name
                    memberOfSpaceIds.forEach { args += LongColumnType() to it.value }
                }
            }

            is SpaceVisibilityScope.Privileged -> {
                parts += "TRUE"
            }
        }
    }

    private fun orderClause(
        sort: SortOption,
        direction: SortDirection
    ): String {
        val primary =
            when (sort) {
                SortOption.LAST_ACTIVITY_AT -> "last_activity_at"
                SortOption.CREATED_AT -> "s.created_at"
                SortOption.NAME -> "s.name"
            }
        return "$primary ${direction.name}, s.id DESC"
    }

    private data class SqlFragment(
        val sql: String,
        val args: List<Pair<IColumnType<*>, Any?>>
    )
}

internal fun decodeSpaceVisibility(stored: String): SpaceVisibility =
    runCatching { stored.asSpaceVisibility() }
        .getOrElse { cause ->
            throw IllegalStateException("저장된 visibility 값을 해석할 수 없습니다.", cause)
        }
