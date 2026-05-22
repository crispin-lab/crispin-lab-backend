package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Request
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import java.time.Instant

interface SpaceListing : UseCase<Request, PageResult<Summary>> {
    class Request(
        page: Int = 0,
        size: Int = DEFAULT_SIZE,
        val viewer: Viewer
    ) {
        val pageRequest: PageRequest =
            PageRequest(
                page = page,
                size = size
            )
    }

    data class Summary(
        val spaceId: SpaceId,
        val name: String,
        val description: String,
        val visibility: SpaceVisibility,
        val createdAt: Instant,
        val updatedAt: Instant
    )
}
