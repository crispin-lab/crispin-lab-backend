package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import java.time.Instant

interface PageSearchPort {
    fun search(
        spaceId: SpaceId,
        keyword: String,
        pageRequest: PageRequest
    ): PageResult<PageSummary>

    data class PageSummary(
        val id: PageId,
        val spaceId: SpaceId,
        val title: String,
        val updatedAt: Instant
    )
}
