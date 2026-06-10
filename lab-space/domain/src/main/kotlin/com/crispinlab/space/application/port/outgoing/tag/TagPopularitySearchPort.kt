package com.crispinlab.space.application.port.outgoing.tag

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.tag.TagPopularitySearchPort.TagPopularitySummary

interface TagPopularitySearchPort {
    fun search(
        scope: VisibilityScope,
        pageRequest: PageRequest
    ): PageResult<TagPopularitySummary>

    data class TagPopularitySummary(
        val name: String,
        val usageCount: Long
    )
}
