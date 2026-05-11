package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Request
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Result
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.space.domain.user.UserId
import java.time.Instant

interface SpaceEditing : UseCase<Request, Result> {
    class Request(
        spaceId: String,
        val name: String? = null,
        val description: String? = null,
        // 권한 도입 전까지는 컨트롤러의 헤더 강제 게이트키퍼 역할만 — UseCase 본문에서는 아직 읽지 않는다.
        val currentUserId: UserId
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()

        init {
            require(name != null || description != null) {
                "수정할 필드를 최소 1개 이상 입력해 주세요."
            }
        }
    }

    data class Result(
        val spaceId: String,
        val name: String,
        val description: String,
        val updatedAt: Instant
    )
}
