package com.crispinlab.user.application.port.incoming.auth

import com.crispinlab.common.application.UseCase
import com.crispinlab.user.application.port.incoming.auth.AuthLogin.Request
import com.crispinlab.user.application.port.incoming.auth.AuthLogin.Result
import com.crispinlab.user.domain.session.SessionToken
import com.crispinlab.user.domain.user.UserId

interface AuthLogin : UseCase<Request, Result> {
    class Request(
        val email: String,
        val password: String
    )

    data class Result(
        val userId: UserId,
        val token: SessionToken
    )
}
