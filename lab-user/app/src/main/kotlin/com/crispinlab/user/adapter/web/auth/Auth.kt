package com.crispinlab.user.adapter.web.auth

import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId

class Auth(
    val userId: UserId,
    val role: SystemRole
) {
    val isAdmin: Boolean
        get() = role == SystemRole.ADMIN
}
