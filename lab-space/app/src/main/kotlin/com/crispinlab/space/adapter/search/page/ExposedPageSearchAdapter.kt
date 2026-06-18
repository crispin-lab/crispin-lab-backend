package com.crispinlab.space.adapter.search.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.adapter.persistence.page.Pages
import com.crispinlab.space.adapter.persistence.page.decodeVisibility
import com.crispinlab.space.adapter.persistence.page.toPagesCondition
import com.crispinlab.space.adapter.persistence.space.Spaces
import com.crispinlab.space.adapter.persistence.tag.PageTags
import com.crispinlab.space.adapter.persistence.toPageResult
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.PageSummary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.SortOption
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.compoundAnd
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedPageSearchAdapter : PageSearchPort {
    override fun search(
        keyword: String?,
        spaceId: SpaceId?,
        tagIds: Collection<TagId>,
        sort: SortOption,
        scope: VisibilityScope,
        pageRequest: PageRequest
    ): PageResult<PageSummary> {
        val tagPageIds =
            if (tagIds.isEmpty()) {
                null
            } else {
                val matched = matchedPageIdsByTag(tagIds)
                if (matched.isEmpty()) return PageResult.empty(pageRequest)
                matched
            }

        return baseQuery(keyword, spaceId, tagPageIds, scope.toPagesCondition())
            .toPageResult(pageRequest, *sort.toOrderColumns()) { it.toSummary() }
    }

    private fun matchedPageIdsByTag(tagIds: Collection<TagId>): List<Long> {
        val distinctTagIds = tagIds.map { it.value }.distinct()
        return PageTags
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
            .having { PageTags.tagId.countDistinct() eq distinctTagIds.size.toLong() }
            .map { it[PageTags.pageId] }
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

    private fun String.escapeLike(): String =
        replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

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
