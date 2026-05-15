package com.crispinlab.user.application.port.incoming.user

import com.crispinlab.common.application.UseCase
import com.crispinlab.user.application.port.incoming.user.UserGetting.Request
import com.crispinlab.user.application.port.incoming.user.UserGetting.Result
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.domain.user.UserId.Companion.asUserId
import java.time.Instant

interface UserGetting : UseCase<Request, Result> {
    class Request(
        userId: String,
        val currentUserId: UserId
    ) {
        val userId: UserId = userId.asUserId()
    }

    data class Result(
        val userId: String,
        val email: String,
        val displayName: String,
        val createdAt: Instant,
        val updatedAt: Instant
    )
}
