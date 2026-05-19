package com.crispinlab.user.adapter.web.user

import com.crispinlab.user.application.port.incoming.user.UserSigning
import com.crispinlab.user.application.port.incoming.user.UserSigning.Request
import com.crispinlab.user.application.port.incoming.user.UserSigning.Result
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/users")
class UserSigningController(
    private val useCase: UserSigning
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun sign(
        @RequestBody body: Body
    ): Result =
        body
            .toRequest()
            .let {
                useCase.perform(it)
            }

    data class Body(
        val email: String,
        val handle: String,
        val password: String
    ) {
        fun toRequest(): Request =
            Request(
                email = email,
                handle = handle,
                password = password
            )

        override fun toString(): String = "Body(email=$email, handle=$handle, password=***)"
    }
}
