package com.crispinlab.space.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.page.PageRevisionListing.Request
import com.crispinlab.space.application.port.incoming.page.PageRevisionListing.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface PageRevisionListing : UseCase<Request, PageResult<Summary>> {
    class Request(
        pageId: String,
        page: Int = 0,
        size: Int = DEFAULT_SIZE,
        val viewer: Viewer
    ) {
        val pageId: PageId = pageId.asPageId()
        val pageRequest: PageRequest =
            PageRequest(
                page = page,
                size = size
            )
    }

    data class Summary(
        val revisionId: PageRevisionId,
        val pageId: PageId,
        val version: Int,
        val title: String,
        val authorId: UserId,
        val createdAt: Instant
    )
}
