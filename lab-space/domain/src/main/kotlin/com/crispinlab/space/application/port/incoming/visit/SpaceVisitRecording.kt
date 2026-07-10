package com.crispinlab.space.application.port.incoming.visit

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.visit.SpaceVisitRecording.Request
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId

interface SpaceVisitRecording : UseCase<Request, Unit> {
    class Request(
        spaceId: String,
        val viewer: Viewer.Member
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
    }
}
