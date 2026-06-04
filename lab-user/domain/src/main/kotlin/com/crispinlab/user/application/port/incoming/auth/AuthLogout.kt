package com.crispinlab.user.application.port.incoming.auth

import com.crispinlab.common.application.UseCase
import com.crispinlab.user.application.port.incoming.auth.AuthLogout.Request
import com.crispinlab.user.domain.session.SessionToken

interface AuthLogout : UseCase<Request, Unit> {
    class Request(
        val token: SessionToken
    )
}
