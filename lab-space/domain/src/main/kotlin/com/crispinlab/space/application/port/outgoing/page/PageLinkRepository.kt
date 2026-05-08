package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageLink
import com.crispinlab.space.domain.page.PageRevisionId

/**
 * PageLink 는 revision 단위 스냅샷으로 보관한다.
 * 매 update 마다 새 revisionId 로 saveAll 되며, 과거 revision 의 링크는 그대로 남는다.
 * "현재 active 링크" = 최신 revision 의 링크 = `findByRevisionId(page.currentVersion 의 revisionId)`.
 */
interface PageLinkRepository {
    fun saveAll(links: List<PageLink>): List<PageLink>

    fun findByPageId(pageId: PageId): List<PageLink>

    fun findByRevisionId(revisionId: PageRevisionId): List<PageLink>
}
