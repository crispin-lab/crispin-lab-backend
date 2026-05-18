package com.crispinlab.user.application.port.incoming.user

import com.crispinlab.common.application.UseCase
import com.crispinlab.user.application.port.incoming.user.UserRegistering.Request
import com.crispinlab.user.application.port.incoming.user.UserRegistering.Result
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.UserId

interface UserRegistering : UseCase<Request, Result> {
    class Request(
        email: String,
        val displayName: String,
        val currentUserId: UserId
    ) {
        val email: EmailAddress = EmailAddress(email)
    }

    data class Result(
        val userId: UserId
    )
}
