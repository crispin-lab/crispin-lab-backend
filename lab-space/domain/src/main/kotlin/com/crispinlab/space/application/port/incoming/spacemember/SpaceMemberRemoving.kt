package com.crispinlab.space.application.port.incoming.spacemember

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRemoving.Request
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.domain.user.UserId.Companion.asUserId

interface SpaceMemberRemoving : UseCase<Request, Unit> {
    class Request(
        spaceId: String,
        targetUserId: String,
        val viewer: Viewer.Member
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
        val targetUserId: UserId = targetUserId.asUserId()
    }
}
