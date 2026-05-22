package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting.Request
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId

interface SpaceDeleting : UseCase<Request, Unit> {
    class Request(
        spaceId: String,
        val viewer: Viewer.Member
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
    }
}
