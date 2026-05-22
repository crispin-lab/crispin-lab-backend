package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Request
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Result
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.space.SpaceVisibility.Companion.asSpaceVisibility
import com.crispinlab.user.domain.user.UserId

interface SpaceRegistering : UseCase<Request, Result> {
    class Request(
        val name: String,
        val description: String,
        visibility: String,
        val currentUserId: UserId
    ) {
        val visibility: SpaceVisibility = visibility.asSpaceVisibility()
    }

    data class Result(
        val spaceId: SpaceId
    )
}
