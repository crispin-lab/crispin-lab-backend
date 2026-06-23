package com.crispinlab.user.application.port.incoming.user

import com.crispinlab.common.application.UseCase
import com.crispinlab.user.application.port.incoming.user.UserSigning.Request
import com.crispinlab.user.application.port.incoming.user.UserSigning.Result
import com.crispinlab.user.domain.credential.Password
import com.crispinlab.user.domain.session.SessionToken
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId

interface UserSigning : UseCase<Request, Result> {
    class Request(
        email: String,
        handle: String,
        password: String
    ) {
        val email: EmailAddress = EmailAddress(email)
        val handle: Handle = Handle(handle)
        val password: Password.Outcome = Password.parse(password)
    }

    data class Result(
        val userId: UserId,
        val token: SessionToken
    )
}
