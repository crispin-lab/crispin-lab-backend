package com.crispinlab.user.adapter.web.session

import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.application.port.incoming.auth.AuthLogout
import com.crispinlab.user.application.port.incoming.auth.AuthLogout.Request
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/sessions")
class SessionDeletionController(
    private val useCase: AuthLogout
) {
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMine(auth: Auth) {
        Request(token = auth.sessionToken)
            .let {
                useCase.perform(it)
            }
    }
}
