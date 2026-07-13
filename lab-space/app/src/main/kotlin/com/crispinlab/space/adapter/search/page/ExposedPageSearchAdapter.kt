package com.crispinlab.space.adapter.search.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.persistence.escapeLike
import com.crispinlab.space.adapter.persistence.page.Pages
import com.crispinlab.space.adapter.persistence.page.decodeVisibility
import com.crispinlab.space.adapter.persistence.space.Spaces
import com.crispinlab.space.adapter.persistence.tag.PageTags
import com.crispinlab.space.adapter.persistence.toPageResult
import com.crispinlab.space.adapter.persistence.visibility.toClauses
import com.crispinlab.space.adapter.persistence.visibility.toExposedOp
import com.crispinlab.space.adapter.persistence.visibility.toSqlFragment
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.LatestPage
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.PageStat
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.PageSummary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.SortOption
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.compoundAnd
import org.jetbrains.exposed.v1.core.compoundOr
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.springframework.stereotype.Repository

@Repository
class ExposedPageSearchAdapter : PageSearchPort {
    override fun search(
        keyword: String?,
        spaceId: SpaceId?,
        tagIds: Collection<TagId>,
        tagIdsAnyOf: Collection<TagId>,
        parentPageId: PageId?,
        onlyRoot: Boolean,
        sort: SortOption,
        scope: VisibilityScope,
        pageRequest: PageRequest
    ): PageResult<PageSummary> {
        require(!(onlyRoot && parentPageId != null)) {
            "parentPageId 와 onlyRoot 는 동시에 지정할 수 없습니다."
        }
        val tagPageIds =
            if (tagIds.isEmpty()) {
                null
            } else {
                val matched = matchedPageIdsByTag(tagIds)
                if (matched.isEmpty()) return PageResult.empty(pageRequest)
                matched
            }
        val anyOfPageIds =
            if (tagIdsAnyOf.isEmpty()) {
                null
            } else {
                val matched = matchedPageIdsByAnyTag(tagIdsAnyOf)
                if (matched.isEmpty()) return PageResult.empty(pageRequest)
                matched
            }

        return baseQuery(
            keyword,
            spaceId,
            tagPageIds,
            anyOfPageIds,
            parentPageId,
            onlyRoot,
            scope.toClauses().toExposedOp()
        ).toPageResult(pageRequest, *sort.toOrderColumns()) { it.toSummary() }
    }

    override fun statsBySpaceIds(
        spaceIds: Collection<SpaceId>,
        scope: VisibilityScope
    ): Map<SpaceId, PageStat> {
        if (spaceIds.isEmpty()) return emptyMap()
        val rawSpaceIds = spaceIds.map { it.value }.distinct()
        val latests = latestsBySpaceIds(rawSpaceIds, scope)
        return countsBySpaceIds(rawSpaceIds, scope)
            .mapKeys { SpaceId(it.key) }
            .mapValues { (spaceId, count) ->
                PageStat(count = count, latest = latests[spaceId.value])
            }
    }

    override fun updatedCountsSince(
        sinceOf: Map<SpaceId, Instant>,
        scope: VisibilityScope
    ): Map<SpaceId, Long> {
        if (sinceOf.isEmpty()) return emptyMap()
        val rawSpaceIds = sinceOf.keys.map { it.value }.distinct()
        val tuplePredicate =
            sinceOf.entries
                .map { (spaceId, since) ->
                    (Pages.spaceId eq spaceId.value) and (Pages.updatedAt greater since)
                }.compoundOr()
        val countColumn = Pages.id.count()
        return Pages
            .join(
                otherTable = Spaces,
                joinType = JoinType.INNER,
                additionalConstraint = { Pages.spaceId eq Spaces.id }
            ).select(Pages.spaceId, countColumn)
            .where {
                (Pages.spaceId inList rawSpaceIds) and
                    Pages.notDeleted() and
                    Spaces.deletedAt.isNull() and
                    scope.toClauses().toExposedOp() and
                    tuplePredicate
            }.groupBy(Pages.spaceId)
            .associate { SpaceId(it[Pages.spaceId]) to it[countColumn] }
    }

    private fun countsBySpaceIds(
        rawSpaceIds: List<Long>,
        scope: VisibilityScope
    ): Map<Long, Long> {
        val countColumn = Pages.id.count()
        return Pages
            .join(
                otherTable = Spaces,
                joinType = JoinType.INNER,
                additionalConstraint = { Pages.spaceId eq Spaces.id }
            ).select(Pages.spaceId, countColumn)
            .where {
                (Pages.spaceId inList rawSpaceIds) and
                    Pages.notDeleted() and
                    Spaces.deletedAt.isNull() and
                    scope.toClauses().toExposedOp()
            }.groupBy(Pages.spaceId)
            .associate { it[Pages.spaceId] to it[countColumn] }
    }

    private fun latestsBySpaceIds(
        rawSpaceIds: List<Long>,
        scope: VisibilityScope
    ): Map<Long, LatestPage> {
        val fragment = scope.toClauses().toSqlFragment()
        val placeholders = rawSpaceIds.joinToString(", ") { "?" }
        val sql =
            """
            SELECT DISTINCT ON (pages.space_id)
                pages.space_id, pages.id, pages.title, pages.updated_at
            FROM pages
            INNER JOIN spaces ON spaces.id = pages.space_id AND spaces.deleted_at IS NULL
            WHERE pages.space_id IN ($placeholders)
              AND pages.deleted_at IS NULL
              AND ${fragment.sql}
            ORDER BY pages.space_id, pages.updated_at DESC, pages.id DESC
            """.trimIndent()
        val args =
            buildList<Pair<IColumnType<*>, Any?>> {
                rawSpaceIds.forEach { add(LongColumnType() to it) }
                addAll(fragment.args)
            }
        return buildMap {
            TransactionManager.current().exec(
                stmt = sql,
                args = args,
                explicitStatementType = StatementType.SELECT
            ) { rs ->
                while (rs.next()) {
                    put(
                        rs.getLong("space_id"),
                        LatestPage(
                            pageId = PageId(rs.getLong("id")),
                            title = rs.getString("title"),
                            updatedAt = rs.getTimestamp("updated_at").toInstant()
                        )
                    )
                }
            }
        }
    }

    private fun matchedPageIdsByTag(tagIds: Collection<TagId>): List<Long> =
        matchedPageIdsBy(tagIds, requireAllMatch = true)

    private fun matchedPageIdsByAnyTag(tagIdsAnyOf: Collection<TagId>): List<Long> =
        matchedPageIdsBy(tagIdsAnyOf, requireAllMatch = false)

    private fun matchedPageIdsBy(
        tagIds: Collection<TagId>,
        requireAllMatch: Boolean
    ): List<Long> {
        val distinctTagIds = tagIds.map { it.value }.distinct()
        val grouped =
            PageTags
                .innerJoin(Pages)
                .join(
                    otherTable = Spaces,
                    joinType = JoinType.INNER,
                    additionalConstraint = { Pages.spaceId eq Spaces.id }
                ).select(PageTags.pageId)
                .where {
                    (PageTags.tagId inList distinctTagIds) and
                        Pages.notDeleted() and
                        Spaces.deletedAt.isNull()
                }.groupBy(PageTags.pageId)
        val matched =
            if (requireAllMatch) {
                grouped.having {
                    PageTags.tagId.countDistinct() eq distinctTagIds.size.toLong()
                }
            } else {
                grouped
            }
        return matched.map { it[PageTags.pageId] }
    }

    private fun SortOption.toOrderColumns(): Array<Pair<Expression<*>, SortOrder>> =
        when (this) {
            SortOption.CREATED_AT -> {
                arrayOf(
                    Pages.createdAt to SortOrder.DESC,
                    Pages.id to SortOrder.DESC
                )
            }

            SortOption.UPDATED_AT, SortOption.RELEVANCE -> {
                arrayOf(
                    Pages.updatedAt to SortOrder.DESC,
                    Pages.id to SortOrder.DESC
                )
            }

            SortOption.TREE -> {
                arrayOf(
                    Pages.parentPageId to SortOrder.ASC_NULLS_FIRST,
                    Pages.displayOrder to SortOrder.ASC,
                    Pages.id to SortOrder.ASC
                )
            }
        }

    private fun baseQuery(
        keyword: String?,
        spaceId: SpaceId?,
        tagPageIds: List<Long>?,
        anyOfPageIds: List<Long>?,
        parentPageId: PageId?,
        onlyRoot: Boolean,
        visibilityCondition: Op<Boolean>
    ): Query {
        val conditions =
            buildList<Op<Boolean>> {
                add(Pages.notDeleted())
                add(Spaces.deletedAt.isNull())
                add(visibilityCondition)
                keyword?.let {
                    val pattern = "%${it.lowercase().escapeLike()}%"
                    add(
                        (Pages.title.lowerCase() like pattern) or
                            (Pages.content.lowerCase() like pattern)
                    )
                }
                spaceId?.let { add(Pages.spaceId eq it.value) }
                tagPageIds?.let { add(Pages.id inList it) }
                anyOfPageIds?.let { add(Pages.id inList it) }
                if (onlyRoot) add(Pages.parentPageId.isNull())
                parentPageId?.let { add(Pages.parentPageId eq it.value) }
            }
        val combined = conditions.compoundAnd()
        return Pages
            .join(
                otherTable = Spaces,
                joinType = JoinType.INNER,
                additionalConstraint = { Pages.spaceId eq Spaces.id }
            ).selectAll()
            .where { combined }
    }

    private fun ResultRow.toSummary(): PageSummary =
        PageSummary(
            id = PageId(this[Pages.id]),
            spaceId = SpaceId(this[Pages.spaceId]),
            parentPageId = this[Pages.parentPageId]?.let(::PageId),
            authorId = UserId(this[Pages.authorId]),
            title = this[Pages.title],
            visibility = decodeVisibility(this[Pages.visibility]),
            displayOrder = this[Pages.displayOrder],
            updatedAt = this[Pages.updatedAt]
        )
}
