package com.crispinlab.user.application.port.outgoing.session

import com.crispinlab.user.domain.session.SessionToken
import com.crispinlab.user.domain.user.UserId

interface SessionService {
    fun issue(userId: UserId): SessionToken

    fun find(token: SessionToken): UserId?

    fun revoke(token: SessionToken)
}
