package com.crispinlab.user.adapter.web.user

import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.application.port.incoming.user.UserSearching
import com.crispinlab.user.application.port.incoming.user.UserSearching.Request
import com.crispinlab.user.application.port.incoming.user.UserSearching.Request.Companion.DEFAULT_SIZE
import com.crispinlab.user.application.port.incoming.user.UserSearching.Result
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/users")
class UserSearchingController(
    private val useCase: UserSearching
) {
    @GetMapping
    fun search(
        @RequestParam query: String,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        @Suppress("UNUSED_PARAMETER") auth: Auth
    ): Result =
        Request(query = query, size = size)
            .let { useCase.perform(it) }
}
