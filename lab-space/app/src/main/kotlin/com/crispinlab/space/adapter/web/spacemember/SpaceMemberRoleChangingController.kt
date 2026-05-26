package com.crispinlab.space.adapter.web.spacemember

import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRoleChanging
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRoleChanging.Request
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRoleChanging.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/spaces/{spaceId}/members/{userId}")
class SpaceMemberRoleChangingController(
    private val useCase: SpaceMemberRoleChanging
) {
    @PutMapping
    fun changeRole(
        @PathVariable spaceId: String,
        @PathVariable userId: String,
        @RequestBody body: Body,
        auth: Auth
    ): Result =
        body
            .toRequestWith(
                spaceId = spaceId,
                userId = userId,
                viewer = auth.toMember()
            ).let { useCase.perform(it) }

    data class Body(
        val role: String
    ) {
        fun toRequestWith(
            spaceId: String,
            userId: String,
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
