package com.crispinlab.space.adapter.web.space

import com.crispinlab.space.application.port.incoming.space.SpaceEditing
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Request
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Result
import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/spaces/{spaceId}")
class SpaceEditingController(
    private val useCase: SpaceEditing
) {
    @PutMapping
    fun edit(
        @PathVariable spaceId: String,
        @RequestBody body: Body,
        auth: Auth
    ): Result =
        body
            .toRequestWith(spaceId = spaceId, userId = auth.userId, role = auth.role)
            .let { useCase.perform(it) }

    data class Body(
        val name: String? = null,
        val description: String? = null,
        val visibility: String? = null
    ) {
        fun toRequestWith(
            spaceId: String,
            userId: UserId,
            role: SystemRole
        ): Request =
            Request(
                spaceId = spaceId,
                name = name,
                description = description,
                visibility = visibility,
                currentUserId = userId,
                currentUserRole = role
            )
    }
}
