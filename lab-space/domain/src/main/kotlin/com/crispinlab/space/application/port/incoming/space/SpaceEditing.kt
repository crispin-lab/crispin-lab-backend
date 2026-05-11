package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.space.application.port.incoming.UseCase
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Request
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Result
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.domain.user.UserId.Companion.asUserId
import java.time.Instant

interface SpaceEditing : UseCase<Request, Result> {
    class Request(
        spaceId: String,
        val name: String? = null,
        val description: String? = null,
        currentUserId: String
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
        val currentUserId: UserId = currentUserId.asUserId()
    }

    data class Result(
        val spaceId: String,
        val name: String,
        val description: String,
        val updatedAt: Instant
    )
}
