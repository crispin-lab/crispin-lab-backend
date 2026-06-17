package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.PageSummary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.PageId

interface PageInboundLinkPort {
    fun findInboundLinksOf(
        targetPageId: PageId,
        scope: VisibilityScope,
        pageRequest: PageRequest
    ): PageResult<PageSummary>
}
