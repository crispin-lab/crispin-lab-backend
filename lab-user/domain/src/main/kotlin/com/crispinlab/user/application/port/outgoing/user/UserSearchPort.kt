package com.crispinlab.user.application.port.outgoing.user

import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId

interface UserSearchPort {
    fun search(
        query: String,
        size: Int
    ): List<Match>

    data class Match(
        val userId: UserId,
        val handle: Handle
    )
}
