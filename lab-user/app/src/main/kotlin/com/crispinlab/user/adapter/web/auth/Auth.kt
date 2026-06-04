package com.crispinlab.user.adapter.web.auth

import com.crispinlab.user.domain.session.SessionToken
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId

class Auth(
    val userId: UserId,
    val role: SystemRole,
    val sessionToken: SessionToken
) {
    val isAdmin: Boolean
        get() = role == SystemRole.ADMIN
}
