package com.crispinlab.user.application.port.outgoing.credential

import com.crispinlab.user.domain.credential.Password

interface PasswordBlocklistPort {
    fun isBlocked(password: Password): Boolean
}
