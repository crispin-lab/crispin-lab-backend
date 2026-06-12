package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.application.port.outgoing.page.PageLinkRepository
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageLink
import com.crispinlab.space.domain.page.PageLinkId
import com.crispinlab.space.domain.page.PageRevisionId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedPageLinkRepository : PageLinkRepository {
    override fun saveAll(links: List<PageLink>): List<PageLink> {
        if (links.isEmpty()) return links
        PageLinks.batchInsert(links) { link ->
            this[PageLinks.id] = link.id.value
            this[PageLinks.pageId] = link.pageId.value
            this[PageLinks.revisionId] = link.revisionId.value
            this[PageLinks.targetPageId] = link.target.value
            this[PageLinks.createdAt] = link.createdAt
        }
        return links
    }

    override fun findByPageId(pageId: PageId): List<PageLink> =
        PageLinks
            .selectAll()
            .where { PageLinks.pageId eq pageId.value }
            .map { it.toEntity() }

    override fun findByRevisionId(revisionId: PageRevisionId): List<PageLink> =
        PageLinks
            .selectAll()
            .where { PageLinks.revisionId eq revisionId.value }
            .map { it.toEntity() }

    private fun ResultRow.toEntity(): PageLink =
        PageLink(
            id = PageLinkId(this[PageLinks.id]),
            pageId = PageId(this[PageLinks.pageId]),
            revisionId = PageRevisionId(this[PageLinks.revisionId]),
            target = PageId(this[PageLinks.targetPageId]),
            createdAt = this[PageLinks.createdAt]
        )
}
