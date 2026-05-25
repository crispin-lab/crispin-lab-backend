package com.crispinlab.space.adapter.web.tag

import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.tag.TagRegistering
import com.crispinlab.space.application.port.incoming.tag.TagRegistering.Request
import com.crispinlab.space.application.port.incoming.tag.TagRegistering.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/tags")
class TagRegisteringController(
    private val useCase: TagRegistering
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @RequestBody body: Body,
        auth: Auth
    ): Result =
        body
            .toRequestWith(viewer = auth.toMember())
            .let {
                useCase.perform(it)
            }

    data class Body(
        val spaceId: String,
        val name: String
    ) {
        fun toRequestWith(viewer: Viewer.Member): Request =
            Request(
                spaceId = spaceId,
                name = name,
                viewer = viewer
            )
    }
}
