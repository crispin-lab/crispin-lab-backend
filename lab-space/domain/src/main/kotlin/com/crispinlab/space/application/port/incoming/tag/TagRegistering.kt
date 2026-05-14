package com.crispinlab.space.application.port.incoming.tag

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.tag.TagRegistering.Request
import com.crispinlab.space.application.port.incoming.tag.TagRegistering.Result
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.space.domain.user.UserId

interface TagRegistering : UseCase<Request, Result> {
    class Request(
        spaceId: String,
        val name: String,
        val currentUserId: UserId
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
    }

    data class Result(
        val tagId: String
    )
}
