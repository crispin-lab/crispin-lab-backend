package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Request
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import java.time.Instant

interface SpaceGetting : UseCase<Request, Result> {
    class Request(
        spaceId: String,
        val viewer: Viewer
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
    }

    data class Result(
        val spaceId: SpaceId,
        val name: String,
        val description: String,
        val visibility: SpaceVisibility,
        val canWrite: Boolean,
        val canEdit: Boolean,
        val viewerRole: SpaceMemberRole?,
        val createdAt: Instant,
        val updatedAt: Instant
    )
}
