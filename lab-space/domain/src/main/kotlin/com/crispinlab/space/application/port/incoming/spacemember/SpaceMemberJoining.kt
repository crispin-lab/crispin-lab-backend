package com.crispinlab.space.application.port.incoming.spacemember

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberJoining.Request
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberJoining.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.domain.spacemember.SpaceMemberRole.Companion.asSpaceMemberRole
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.domain.user.UserId.Companion.asUserId

interface SpaceMemberJoining : UseCase<Request, Result> {
    class Request(
        spaceId: String,
        targetUserId: String? = null,
        role: String? = null,
        val viewer: Viewer.Member
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
        val targetUserId: UserId? = targetUserId?.asUserId()
        val role: SpaceMemberRole? = role?.asSpaceMemberRole()
    }

    data class Result(
        val spaceMemberId: SpaceMemberId,
        val spaceId: SpaceId,
        val userId: UserId,
        val role: SpaceMemberRole
    )
}
