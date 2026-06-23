package com.crispinlab.user.application.port.outgoing.credential

import com.crispinlab.user.domain.credential.Password
import com.crispinlab.user.domain.credential.PasswordHash

interface PasswordEncoder {
    fun encode(password: Password): PasswordHash

    fun matches(
        raw: String,
        hash: PasswordHash
    ): Boolean
}
