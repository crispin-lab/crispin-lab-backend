package com.crispinlab.user.adapter.web.auth

import com.crispinlab.user.application.port.incoming.auth.AuthLogout
import com.crispinlab.user.application.port.incoming.auth.AuthLogout.Request
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/auth/logout")
class AuthLogoutController(
    private val useCase: AuthLogout
) {
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(
        @RequestBody body: Body
    ) {
        useCase.perform(body.toRequest())
    }

    data class Body(
        val token: String
    ) {
        fun toRequest(): Request = Request(token = token)

        override fun toString(): String = "Body(token=***)"
    }
}
