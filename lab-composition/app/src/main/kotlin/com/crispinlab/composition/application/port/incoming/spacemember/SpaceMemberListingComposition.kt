package com.crispinlab.composition.application.port.incoming.spacemember

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.incoming.spacemember.SpaceMemberListingComposition.Request
import com.crispinlab.composition.application.port.incoming.spacemember.SpaceMemberListingComposition.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface SpaceMemberListingComposition : UseCase<Request, PageResult<Result>> {
    class Request(
        val spaceId: String,
        val page: Int,
        val size: Int,
        val viewer: Viewer
    )

    data class Result(
        val spaceMemberId: SpaceMemberId,
        val spaceId: SpaceId,
        val userId: UserId,
        val role: SpaceMemberRole,
        val joinedAt: Instant,
        val handle: String
    )
}
