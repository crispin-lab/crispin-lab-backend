package com.crispinlab.composition.adapter.web.user

import com.crispinlab.composition.application.port.incoming.user.UserSearchingComposition
import com.crispinlab.composition.application.port.incoming.user.UserSearchingComposition.Request
import com.crispinlab.composition.application.port.incoming.user.UserSearchingComposition.Result
import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.application.port.incoming.user.UserSearching.Request.Companion.DEFAULT_SIZE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/users")
class UserSearchingCompositionController(
    private val useCase: UserSearchingComposition
) {
    @GetMapping
    fun search(
        @RequestParam query: String,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        @RequestParam(required = false) spaceId: String?,
        auth: Auth
    ): Result =
        Request(
            query = query,
            size = size,
            spaceId = spaceId,
            viewer = auth.toMember()
        ).let { useCase.perform(it) }
}
