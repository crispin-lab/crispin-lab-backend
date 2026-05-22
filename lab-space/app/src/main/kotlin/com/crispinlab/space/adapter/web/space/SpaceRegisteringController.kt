package com.crispinlab.space.adapter.web.space

import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Request
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/spaces")
class SpaceRegisteringController(
    private val useCase: SpaceRegistering
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @RequestBody body: Body,
        auth: Auth
    ): Result =
        body
            .toRequestWith(auth.toMember())
            .let { useCase.perform(it) }

    data class Body(
        val name: String,
        val description: String,
        val visibility: String? = null
    ) {
        fun toRequestWith(viewer: Viewer.Member): Request =
            Request(
                name = name,
                description = description,
                visibility = visibility ?: "INTERNAL",
                viewer = viewer
            )
    }
}
