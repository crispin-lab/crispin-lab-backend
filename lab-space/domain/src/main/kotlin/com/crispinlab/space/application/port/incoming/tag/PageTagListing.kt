package com.crispinlab.space.application.port.incoming.tag

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.tag.PageTagListing.Request
import com.crispinlab.space.application.port.incoming.tag.PageTagListing.Summary
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.domain.user.UserId
import java.time.Instant

interface PageTagListing : UseCase<Request, PageResult<Summary>> {
    class Request(
        pageId: String,
        page: Int = 0,
        size: Int = DEFAULT_SIZE,
        val currentUserId: UserId
    ) {
        val pageId: PageId = pageId.asPageId()
        val pageRequest: PageRequest =
            PageRequest(
                page = page,
                size = size
            )
    }

    data class Summary(
        val tagId: TagId,
        val spaceId: SpaceId,
        val name: String,
        val createdAt: Instant
    )
}
