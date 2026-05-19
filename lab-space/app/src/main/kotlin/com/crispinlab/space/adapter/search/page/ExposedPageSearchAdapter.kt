package com.crispinlab.space.adapter.search.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.adapter.persistence.page.Pages
import com.crispinlab.space.adapter.persistence.tag.PageTags
import com.crispinlab.space.adapter.persistence.toPageResult
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.PageSummary
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagId
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.compoundAnd
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
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

        return baseQuery(keyword, spaceId, tagPageIds)
            .toPageResult(
                pageRequest,
                Pages.updatedAt to SortOrder.DESC,
                Pages.id to SortOrder.DESC
            ) { it.toSummary() }
    }

    /*
    todo    :: 한 태그에 매칭되는 page_id 수에 상한이 없어 메모리·IN 절 폭주 가능. ES 어댑터 교체 시 자연 해결.
     author :: heechoel shin
     date   :: 2026-05-15T17:30:00KST
     ticket :: LAB-25
     */
    private fun matchedPageIdsByTag(tagIds: Collection<TagId>): List<Long> =
        (PageTags innerJoin Pages)
            .select(PageTags.pageId)
            .where {
                (PageTags.tagId inList tagIds.map { it.value }) and Pages.notDeleted()
            }.withDistinct()
            .map { it[PageTags.pageId] }

    private fun baseQuery(
        keyword: String?,
        spaceId: SpaceId?,
        tagPageIds: List<Long>?
    ): Query {
        val conditions =
            buildList<Op<Boolean>> {
                add(Pages.notDeleted())
                add(Pages.visibility eq Visibility.PUBLIC.name)
                keyword?.let {
                    val pattern = "%${it.escapeLike()}%"
                    add((Pages.title like pattern) or (Pages.content like pattern))
                }
                spaceId?.let { add(Pages.spaceId eq it.value) }
                tagPageIds?.let { add(Pages.id inList it) }
            }
        val combined = conditions.compoundAnd()
        return Pages.selectAll().where { combined }
    }

    // Postgres LIKE 의 default escape 문자(`\`)를 활용해 사용자 키워드 안의 wildcard 를 literal 로 만든다.
    private fun String.escapeLike(): String =
        replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    private fun ResultRow.toSummary(): PageSummary =
        PageSummary(
            id = PageId(this[Pages.id]),
            spaceId = SpaceId(this[Pages.spaceId]),
            title = this[Pages.title],
            updatedAt = this[Pages.updatedAt]
        )
}
