package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.space.application.port.incoming.UseCase
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Request
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Result
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import java.time.Instant

interface SpaceEditing : UseCase<Request, Result> {
    class Request(
        spaceId: String,
        val name: String? = null,
        val description: String? = null
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
    }

    data class Result(
        val spaceId: Long,
        val name: String,
        val description: String,
        val updatedAt: Instant
    )
}
