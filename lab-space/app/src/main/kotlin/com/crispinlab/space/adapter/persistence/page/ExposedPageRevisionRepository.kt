package com.crispinlab.space.adapter.persistence.page

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
class ExposedPageRevisionRepository : PageRevisionRepository {
    override fun save(revision: PageRevision): PageRevision =
        PageRevisions
            .selectAll()
            .where { PageRevisions.id eq revision.id.value }
            .firstOrNull()
            ?.let { update(revision) }
            ?: insert(revision)

    override fun findBy(id: PageRevisionId): PageRevision? =
        PageRevisions
            .selectAll()
            .where { PageRevisions.id eq id.value }
            .firstOrNull()
            ?.toEntity()

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

    private fun insert(revision: PageRevision): PageRevision =
        revision.also {
            PageRevisions.insert {
                it[id] = revision.id.value
                it[pageId] = revision.pageId.value
                it[version] = revision.version
                it[title] = revision.title
                it[content] = revision.content.raw
                it[authorId] = revision.authorId.value
                it[createdAt] = revision.createdAt
            }
        }

    private fun update(revision: PageRevision): PageRevision =
        revision.also {
            PageRevisions.update({ PageRevisions.id eq revision.id.value }) {
                it[pageId] = revision.pageId.value
                it[version] = revision.version
                it[title] = revision.title
                it[content] = revision.content.raw
                it[authorId] = revision.authorId.value
                it[createdAt] = revision.createdAt
            }
        }

    private fun ResultRow.toEntity(): PageRevision =
        PageRevision(
            id = PageRevisionId(this[PageRevisions.id]),
            pageId = PageId(this[PageRevisions.pageId]),
            version = this[PageRevisions.version],
            title = this[PageRevisions.title],
            content = PageContent(this[PageRevisions.content]),
            authorId = UserId(this[PageRevisions.authorId]),
            createdAt = this[PageRevisions.createdAt]
        )
}
