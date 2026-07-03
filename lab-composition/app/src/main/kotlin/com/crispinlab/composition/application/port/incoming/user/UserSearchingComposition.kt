package com.crispinlab.composition.application.port.incoming.user

import com.crispinlab.common.application.UseCase
import com.crispinlab.composition.application.port.incoming.user.UserSearchingComposition.Request
import com.crispinlab.composition.application.port.incoming.user.UserSearchingComposition.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId

interface UserSearchingComposition : UseCase<Request, Result> {
    class Request(
        val query: String,
        val size: Int,
        val viewer: Viewer.Member
    )

    data class Result(
        val items: List<Item>
    ) {
        data class Item(
            val userId: UserId,
            val handle: Handle,
            val memberOfSpaceIds: List<SpaceId>
        )
    }
}
