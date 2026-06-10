package com.crispinlab.space.application.port.incoming.tag

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing.Request
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing.Summary
import com.crispinlab.space.domain.access.Viewer

interface TagPopularityListing : UseCase<Request, PageResult<Summary>> {
    class Request(
        page: Int = 0,
        size: Int = DEFAULT_POPULAR_SIZE,
        val viewer: Viewer
    ) {
        init {
            require(size <= MAX_POPULAR_SIZE) {
                "인기 태그 페이지 크기는 ${MAX_POPULAR_SIZE} 이하여야 합니다."
            }
        }

        val pageRequest: PageRequest =
            PageRequest(
                page = page,
                size = size
            )

        companion object {
            const val DEFAULT_POPULAR_SIZE: Int = 30
            const val MAX_POPULAR_SIZE: Int = 100
        }
    }

    data class Summary(
        val name: String,
        val usageCount: Long
    )
}
