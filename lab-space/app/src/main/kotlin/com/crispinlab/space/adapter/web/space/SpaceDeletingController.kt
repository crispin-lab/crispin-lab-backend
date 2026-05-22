package com.crispinlab.space.adapter.web.space

import com.crispinlab.space.application.port.incoming.space.SpaceDeleting
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting.Request
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/spaces/{spaceId}")
class SpaceDeletingController(
    private val useCase: SpaceDeleting
) {
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable spaceId: String,
        auth: Auth
    ) {
        Request(spaceId = spaceId, currentUserId = auth.userId)
            .let { useCase.perform(it) }
    }
}
