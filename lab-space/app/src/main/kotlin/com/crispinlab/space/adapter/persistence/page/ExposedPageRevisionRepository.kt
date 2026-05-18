package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.adapter.persistence.ExposedEntityRepository
import com.crispinlab.space.application.port.outgoing.page.PageRevisionRepository
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageRevision
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.space.domain.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class ExposedPageRevisionRepository :
    ExposedEntityRepository<PageRevision, PageRevisionId>(),
    PageRevisionRepository {
    override val table = PageRevisions
    override val idColumn = PageRevisions.id
    override val deletedAtColumn = null

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

    override fun insert(entity: PageRevision) {
        PageRevisions.insert {
            it[id] = entity.id.value
            it[pageId] = entity.pageId.value
            it[version] = entity.version
            it[title] = entity.title
            it[content] = entity.content.raw
            it[authorId] = entity.authorId.value
            it[createdAt] = entity.createdAt
        }
    }

    override fun update(entity: PageRevision) {
        PageRevisions.update({ PageRevisions.id eq entity.id.value }) {
            it[pageId] = entity.pageId.value
            it[version] = entity.version
            it[title] = entity.title
            it[content] = entity.content.raw
            it[authorId] = entity.authorId.value
            it[createdAt] = entity.createdAt
        }
    }

    override fun findByPageId(pageId: PageId): List<PageRevision> =
        PageRevisions
            .selectAll()
            .where { PageRevisions.pageId eq pageId.value }
            .orderBy(PageRevisions.version, SortOrder.DESC)
            .map { it.toEntity() }

    override fun findLatestByPageId(pageId: PageId): PageRevision? =
        PageRevisions
            .selectAll()
            .where { PageRevisions.pageId eq pageId.value }
            .orderBy(PageRevisions.version, SortOrder.DESC)
            .firstOrNull()
            ?.toEntity()
}
