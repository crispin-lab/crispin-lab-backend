package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Request
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Summary
import com.crispinlab.space.domain.user.UserId
import java.time.Instant

interface SpaceListing : UseCase<Request, PageResult<Summary>> {
    class Request(
        page: Int = 0,
        size: Int = DEFAULT_SIZE,
        val currentUserId: UserId
    ) {
        val pageRequest: PageRequest =
            PageRequest(
                page = page,
                size = size
            )
    }

    data class Summary(
        val spaceId: String,
        val name: String,
        val description: String,
        val createdAt: Instant,
        val updatedAt: Instant
    )
}
