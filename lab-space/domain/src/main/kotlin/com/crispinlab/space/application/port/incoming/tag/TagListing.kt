package com.crispinlab.space.application.port.incoming.tag

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.tag.TagListing.Request
import com.crispinlab.space.application.port.incoming.tag.TagListing.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.space.domain.tag.TagId
import java.time.Instant

interface TagListing : UseCase<Request, PageResult<Summary>> {
    class Request(
        spaceId: String,
        page: Int = 0,
        size: Int = DEFAULT_SIZE,
        val viewer: Viewer.Member
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
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
