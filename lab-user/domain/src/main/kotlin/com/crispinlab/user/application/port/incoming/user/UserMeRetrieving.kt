package com.crispinlab.user.application.port.incoming.user

import com.crispinlab.common.application.UseCase
import com.crispinlab.user.application.port.incoming.user.UserMeRetrieving.Request
import com.crispinlab.user.application.port.incoming.user.UserMeRetrieving.Result
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.domain.user.UserId.Companion.asUserId

interface UserMeRetrieving : UseCase<Request, Result> {
    class Request(
        currentUserId: String
    ) {
        val currentUserId: UserId = currentUserId.asUserId()
    }

    data class Result(
        val userId: UserId,
        val handle: Handle,
        val email: EmailAddress,
        val isAdmin: Boolean
    )
}
