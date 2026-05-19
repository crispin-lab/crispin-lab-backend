package com.crispinlab.user.application.usecase.auth

import com.crispinlab.user.application.port.incoming.auth.AuthLogout
import com.crispinlab.user.application.port.incoming.auth.AuthLogout.Request
import com.crispinlab.user.application.port.outgoing.session.SessionService
import com.crispinlab.user.domain.session.SessionToken
import org.springframework.stereotype.Service

@Service
class AuthLogoutUseCase(
    private val sessionService: SessionService
) : AuthLogout {
    override fun perform(request: Request) {
        runCatching { SessionToken(request.token) }
            .getOrNull()
            ?.let {
                sessionService.revoke(it)
            }
    }
}
