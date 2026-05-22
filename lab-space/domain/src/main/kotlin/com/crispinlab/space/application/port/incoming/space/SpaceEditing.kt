package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Request
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Result
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.space.SpaceVisibility.Companion.asSpaceVisibility
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface SpaceEditing : UseCase<Request, Result> {
    class Request(
        spaceId: String,
        val name: String? = null,
        val description: String? = null,
        visibility: String? = null,
        val currentUserId: UserId,
        val currentUserRole: SystemRole
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
        val visibility: SpaceVisibility? = visibility?.asSpaceVisibility()

        init {
            require(name != null || description != null || this.visibility != null) {
                "수정할 필드를 최소 1개 이상 입력해 주세요."
            }
        }
    }

    data class Result(
        val spaceId: SpaceId,
        val name: String,
        val description: String,
        val visibility: SpaceVisibility,
        val updatedAt: Instant
    )
}
