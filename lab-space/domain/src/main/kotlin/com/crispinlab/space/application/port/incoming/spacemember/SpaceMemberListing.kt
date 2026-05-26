package com.crispinlab.space.application.port.incoming.spacemember

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberListing.Request
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberListing.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface SpaceMemberListing : UseCase<Request, PageResult<Summary>> {
    class Request(
        spaceId: String,
        page: Int = 0,
        size: Int = DEFAULT_SIZE,
        val viewer: Viewer
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
        val pageRequest: PageRequest =
            PageRequest(
                page = page,
                size = size
            )
    }

    data class Summary(
        val spaceMemberId: SpaceMemberId,
        val spaceId: SpaceId,
        val userId: UserId,
        val role: SpaceMemberRole,
        val joinedAt: Instant
    )
}
