package com.crispinlab.space.adapter.web.spacemember

import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRemoving
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRemoving.Request
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/spaces/{spaceId}/members/{userId}")
class SpaceMemberRemovingController(
    private val useCase: SpaceMemberRemoving
) {
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(
        @PathVariable spaceId: String,
        @PathVariable userId: String,
        auth: Auth
    ) {
        Request(
            spaceId = spaceId,
            targetUserId = userId,
            viewer = auth.toMember()
        ).let { useCase.perform(it) }
    }
}
