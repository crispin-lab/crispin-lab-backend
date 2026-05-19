package com.crispinlab.user.application.port.incoming.user

import com.crispinlab.common.application.UseCase
import com.crispinlab.user.application.port.incoming.user.UserRegistering.Request
import com.crispinlab.user.application.port.incoming.user.UserRegistering.Result
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId

interface UserRegistering : UseCase<Request, Result> {
    class Request(
        email: String,
        handle: String,
        val password: String
    ) {
        val email: EmailAddress = EmailAddress(email)
        val handle: Handle = Handle(handle)

        init {
            require(password.isNotBlank()) {
                "비밀번호를 입력해 주세요."
            }
        }
    }

    data class Result(
        val userId: UserId
    )
}
