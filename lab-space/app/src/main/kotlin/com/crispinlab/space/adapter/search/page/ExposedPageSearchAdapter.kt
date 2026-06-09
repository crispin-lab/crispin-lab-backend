package com.crispinlab.space.adapter.search.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.adapter.persistence.page.Pages
import com.crispinlab.space.adapter.persistence.tag.PageTags
import com.crispinlab.space.adapter.persistence.toPageResult
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.PageSummary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.SortOption
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagId
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.compoundAnd
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
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

        return baseQuery(keyword, spaceId, tagPageIds, scope.toCondition())
            .toPageResult(pageRequest, *sort.toOrderColumns()) { it.toSummary() }
    }

    private fun matchedPageIdsByTag(tagIds: Collection<TagId>): List<Long> {
        val distinctTagIds = tagIds.map { it.value }.distinct()
        return (PageTags innerJoin Pages)
            .select(PageTags.pageId)
            .where {
                (PageTags.tagId inList distinctTagIds) and Pages.notDeleted()
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
        }

    private fun VisibilityScope.toCondition(): Op<Boolean> =
        when (this) {
            is VisibilityScope.Anonymous -> {
                Pages.visibility eq Visibility.PUBLIC.name
            }

            is VisibilityScope.Authenticated -> {
                val publicClause = Pages.visibility eq Visibility.PUBLIC.name
                val draftClause =
                    (Pages.visibility eq Visibility.DRAFT.name) and
                        (Pages.authorId eq viewerId.value)
                if (memberOfSpaceIds.isEmpty()) {
                    publicClause or draftClause
                } else {
                    val internalClause =
                        (Pages.visibility eq Visibility.INTERNAL.name) and
                            (Pages.spaceId inList memberOfSpaceIds.map { it.value })
                    publicClause or internalClause or draftClause
                }
            }

            is VisibilityScope.Privileged -> {
                Op.TRUE
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
        return Pages.selectAll().where { combined }
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
            title = this[Pages.title],
            updatedAt = this[Pages.updatedAt]
        )
}
