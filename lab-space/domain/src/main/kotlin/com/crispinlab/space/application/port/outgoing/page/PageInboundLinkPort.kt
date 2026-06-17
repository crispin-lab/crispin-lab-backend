package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface PageInboundLinkPort {
    fun findInboundLinksOf(
        targetPageId: PageId,
        scope: VisibilityScope,
        pageRequest: PageRequest
    ): PageResult<InboundLinkSummary>

    data class InboundLinkSummary(
        val pageId: PageId,
        val spaceId: SpaceId,
        val parentPageId: PageId?,
        val authorId: UserId,
        val title: String,
        val visibility: Visibility,
        val displayOrder: Int,
        val updatedAt: Instant
    )
}
