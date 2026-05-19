package com.crispinlab.user.adapter.web.auth

import com.crispinlab.user.application.port.incoming.auth.AuthLogin
import com.crispinlab.user.application.port.incoming.auth.AuthLogin.Request
import com.crispinlab.user.application.port.incoming.auth.AuthLogin.Result
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/auth/login")
class AuthLoginController(
    private val useCase: AuthLogin
) {
    @PostMapping
    fun login(
        @RequestBody body: Body
    ): Result =
        body
            .toRequest()
            .let {
                useCase.perform(it)
            }

    data class Body(
        val email: String,
        val password: String
    ) {
        fun toRequest(): Request =
            Request(
                email = email,
                password = password
            )

        override fun toString(): String = "Body(email=***, password=***)"
    }
}
