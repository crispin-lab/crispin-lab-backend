package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.application.port.outgoing.page.PageLinkRepository
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageLink
import com.crispinlab.space.domain.page.PageLink.Target
import com.crispinlab.space.domain.page.PageLinkId
import com.crispinlab.space.domain.page.PageRevisionId
import java.net.URI
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
            when (val target = link.target) {
                is Target.Internal -> {
                    this[PageLinks.targetPageId] = target.targetPageId.value
                    this[PageLinks.targetUrl] = null
                }

                is Target.External -> {
                    this[PageLinks.targetPageId] = null
                    this[PageLinks.targetUrl] = target.url.toString()
                }
            }
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
            target = decodeTarget(),
            createdAt = this[PageLinks.createdAt]
        )

    private fun ResultRow.decodeTarget(): Target {
        val targetPageId: Long? = this[PageLinks.targetPageId]
        val targetUrl: String? = this[PageLinks.targetUrl]
        return when {
            targetPageId != null && targetUrl != null -> {
                throw IllegalStateException(
                    "page_links row 의 target_page_id 와 target_url 이 모두 설정되어 있습니다."
                )
            }

            targetPageId != null -> {
                Target.Internal(PageId(targetPageId))
            }

            targetUrl != null -> {
                Target.External(URI.create(targetUrl))
            }

            else -> {
                throw IllegalStateException(
                    "page_links row 의 target_page_id / target_url 이 모두 비어 있습니다."
                )
            }
        }
    }
}
