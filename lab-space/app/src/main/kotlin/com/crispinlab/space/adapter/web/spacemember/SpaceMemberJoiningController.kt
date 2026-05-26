package com.crispinlab.space.adapter.web.spacemember

import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberJoining
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberJoining.Request
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberJoining.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/spaces/{spaceId}/members")
class SpaceMemberJoiningController(
    private val useCase: SpaceMemberJoining
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun join(
        @PathVariable spaceId: String,
        @RequestBody(required = false) body: Body?,
        auth: Auth
    ): Result =
        (body ?: Body())
            .toRequestWith(spaceId = spaceId, viewer = auth.toMember())
            .let { useCase.perform(it) }

    data class Body(
        val userId: String? = null,
        val role: String? = null
    ) {
        fun toRequestWith(
            spaceId: String,
            viewer: Viewer.Member
        ): Request =
            Request(
                spaceId = spaceId,
                targetUserId = userId,
                role = role,
                viewer = viewer
            )
    }
}
