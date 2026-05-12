package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Request
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Result
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.space.domain.user.UserId
import java.time.Instant

interface SpaceGetting : UseCase<Request, Result> {
    class Request(
        spaceId: String,
        val currentUserId: UserId
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
    }

    data class Result(
        val spaceId: String,
        val name: String,
        val description: String,
        val createdAt: Instant,
        val updatedAt: Instant
    )
}
