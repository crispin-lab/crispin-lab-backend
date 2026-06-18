package com.crispinlab.space.adapter.search.tag

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.tag.TagPopularitySearchPort
import com.crispinlab.space.application.port.outgoing.tag.TagPopularitySearchPort.TagPopularitySummary
import com.crispinlab.space.domain.page.Visibility
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.springframework.stereotype.Repository

@Repository
class ExposedTagPopularitySearchAdapter : TagPopularitySearchPort {
    override fun search(
        scope: VisibilityScope,
        pageRequest: PageRequest
    ): PageResult<TagPopularitySummary> {
        val visibility = scope.toSqlFragment()
        val sql =
            """
            SELECT name, usage_count, total_elements FROM (
                SELECT tags.name AS name,
                       COUNT(*) AS usage_count,
                       COUNT(*) OVER () AS total_elements
                FROM tags
                INNER JOIN page_tags ON page_tags.tag_id = tags.id
                INNER JOIN pages ON pages.id = page_tags.page_id
                WHERE pages.deleted_at IS NULL AND ${visibility.sql}
                GROUP BY tags.name
            ) AS grouped
            ORDER BY usage_count DESC, name ASC
            LIMIT ? OFFSET ?
            """.trimIndent()

        val args: List<Pair<IColumnType<*>, Any?>> =
            buildList {
                addAll(visibility.args)
                add(IntegerColumnType() to pageRequest.size)
                add(LongColumnType() to pageRequest.offset)
            }

        var totalElements: Long = 0
        val items = mutableListOf<TagPopularitySummary>()
        TransactionManager.current().exec(
            stmt = sql,
            args = args,
            explicitStatementType = StatementType.SELECT
        ) { rs ->
            while (rs.next()) {
                totalElements = rs.getLong("total_elements")
                items.add(
                    TagPopularitySummary(
                        name = rs.getString("name"),
                        usageCount = rs.getLong("usage_count")
                    )
                )
            }
        }

        return PageResult(
            items = items,
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = totalElements
        )
    }

    private data class VisibilityFragment(
        val sql: String,
        val args: List<Pair<IColumnType<*>, Any?>>
    )

    private fun VisibilityScope.toSqlFragment(): VisibilityFragment =
        when (this) {
            is VisibilityScope.Anonymous -> {
                VisibilityFragment(
                    sql = "pages.visibility = ?",
                    args = listOf(VarCharColumnType() to Visibility.PUBLIC.name)
                )
            }

            is VisibilityScope.Authenticated -> {
                val clauses = mutableListOf<String>()
                val args = mutableListOf<Pair<IColumnType<*>, Any?>>()

                clauses += "pages.visibility = ?"
                args += VarCharColumnType() to Visibility.PUBLIC.name

                if (memberOfSpaceIds.isNotEmpty()) {
                    val placeholders = memberOfSpaceIds.joinToString(", ") { "?" }
                    clauses += "(pages.visibility = ? AND pages.space_id IN ($placeholders))"
                    args += VarCharColumnType() to Visibility.MEMBER.name
                    memberOfSpaceIds.forEach { args += LongColumnType() to it.value }
                }

                clauses += "(pages.visibility = ? AND pages.author_id = ?)"
                args += VarCharColumnType() to Visibility.INTERNAL.name
                args += LongColumnType() to viewerId.value

                clauses += "(pages.visibility = ? AND pages.author_id = ?)"
                args += VarCharColumnType() to Visibility.DRAFT.name
                args += LongColumnType() to viewerId.value

                VisibilityFragment(
                    sql = "(${clauses.joinToString(" OR ")})",
                    args = args
                )
            }

            is VisibilityScope.Privileged -> {
                VisibilityFragment(
                    sql = "TRUE",
                    args = emptyList()
                )
            }
        }
}
