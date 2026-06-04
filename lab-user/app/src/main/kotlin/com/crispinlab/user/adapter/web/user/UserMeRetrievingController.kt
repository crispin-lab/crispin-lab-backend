package com.crispinlab.user.adapter.web.user

import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.application.port.incoming.user.UserMeRetrieving
import com.crispinlab.user.application.port.incoming.user.UserMeRetrieving.Request
import com.crispinlab.user.application.port.incoming.user.UserMeRetrieving.Result
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/users/me")
class UserMeRetrievingController(
    private val useCase: UserMeRetrieving
) {
    @GetMapping
    fun getMine(auth: Auth): Result =
        Request(currentUserId = auth.userId.value.toString())
            .let {
                useCase.perform(it)
            }
}
