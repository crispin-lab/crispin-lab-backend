package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.persistence.ExposedEntityRepository
import com.crispinlab.space.adapter.persistence.toPageResult
import com.crispinlab.space.application.port.outgoing.page.PageRevisionRepository
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageRevision
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedPageRevisionRepository :
    ExposedEntityRepository<PageRevision, PageRevisionId>(),
    PageRevisionRepository {
    override val table = PageRevisions
    override val idColumn = PageRevisions.id
    override val deletedAtColumn = null
    override val updateExclude = listOf(PageRevisions.id, PageRevisions.createdAt)

    override fun ResultRow.toEntity(): PageRevision =
        PageRevision(
            id = PageRevisionId(this[PageRevisions.id]),
            pageId = PageId(this[PageRevisions.pageId]),
            version = this[PageRevisions.version],
            title = this[PageRevisions.title],
            content = PageContent(this[PageRevisions.content]),
            authorId = UserId(this[PageRevisions.authorId]),
            createdAt = this[PageRevisions.createdAt]
        )

    override fun upsertBody(
        builder: UpsertStatement<Long>,
        entity: PageRevision
    ) {
        builder[PageRevisions.id] = entity.id.value
        builder[PageRevisions.pageId] = entity.pageId.value
        builder[PageRevisions.version] = entity.version
        builder[PageRevisions.title] = entity.title
        builder[PageRevisions.content] = entity.content.raw
        builder[PageRevisions.authorId] = entity.authorId.value
        builder[PageRevisions.createdAt] = entity.createdAt
    }

    override fun findBy(
        pageId: PageId,
        version: Int
    ): PageRevision? =
        PageRevisions
            .selectAll()
            .where { (PageRevisions.pageId eq pageId.value) and (PageRevisions.version eq version) }
            .firstOrNull()
            ?.toEntity()

    override fun findByPageId(
        pageId: PageId,
        pageRequest: PageRequest
    ): PageResult<PageRevision> =
        PageRevisions
            .selectAll()
            .where { PageRevisions.pageId eq pageId.value }
            .toPageResult(
                pageRequest,
                PageRevisions.version to SortOrder.DESC
            ) { it.toEntity() }
}
