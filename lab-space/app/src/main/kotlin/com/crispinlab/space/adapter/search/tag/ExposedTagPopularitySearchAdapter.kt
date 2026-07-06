package com.crispinlab.space.adapter.search.tag

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.adapter.persistence.visibility.toClauses
import com.crispinlab.space.adapter.persistence.visibility.toSqlFragment
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.tag.TagPopularitySearchPort
import com.crispinlab.space.application.port.outgoing.tag.TagPopularitySearchPort.TagPopularitySummary
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.springframework.stereotype.Repository

@Repository
class ExposedTagPopularitySearchAdapter : TagPopularitySearchPort {
    override fun search(
        scope: VisibilityScope,
        pageRequest: PageRequest
    ): PageResult<TagPopularitySummary> {
        val visibility = scope.toClauses().toSqlFragment()
        val sql =
            """
            SELECT name, usage_count, total_elements FROM (
                SELECT tags.name AS name,
                       COUNT(*) AS usage_count,
                       COUNT(*) OVER () AS total_elements
                FROM tags
                INNER JOIN page_tags ON page_tags.tag_id = tags.id
                INNER JOIN pages ON pages.id = page_tags.page_id
                INNER JOIN spaces ON spaces.id = pages.space_id
                WHERE pages.deleted_at IS NULL
                  AND spaces.deleted_at IS NULL
                  AND ${visibility.sql}
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
}
