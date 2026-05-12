package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Request
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Result
import com.crispinlab.space.domain.user.UserId

interface SpaceRegistering : UseCase<Request, Result> {
    class Request(
        val name: String,
        val description: String,
        val currentUserId: UserId
    )

    data class Result(
        val spaceId: String
    )
}
