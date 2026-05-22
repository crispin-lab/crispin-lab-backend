package com.crispinlab.space.adapter.web.space

import com.crispinlab.space.application.port.incoming.space.SpaceGetting
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Request
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Result
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/spaces/{spaceId}")
class SpaceGettingController(
    private val useCase: SpaceGetting
) {
    @GetMapping
    fun get(
        @PathVariable spaceId: String,
        auth: Auth?
    ): Result =
        Request(spaceId = spaceId, currentUserId = auth?.userId)
            .let { useCase.perform(it) }
}
