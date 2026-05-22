package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting.Request
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.user.domain.user.AuthContext

interface SpaceDeleting : UseCase<Request, Unit> {
    class Request(
        spaceId: String,
        val auth: AuthContext.Authenticated
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
    }
}
