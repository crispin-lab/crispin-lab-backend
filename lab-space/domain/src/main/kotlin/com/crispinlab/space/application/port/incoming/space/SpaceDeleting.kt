package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.space.application.port.incoming.UseCase
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting.Request
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId

interface SpaceDeleting : UseCase<Request, Unit> {
    class Request(
        spaceId: String
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
    }
}
