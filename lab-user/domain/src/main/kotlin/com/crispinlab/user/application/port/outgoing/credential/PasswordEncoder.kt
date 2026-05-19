package com.crispinlab.user.application.port.outgoing.credential

import com.crispinlab.user.domain.credential.PasswordHash

interface PasswordEncoder {
    fun encode(raw: String): PasswordHash

    fun matches(
        raw: String,
        hash: PasswordHash
    ): Boolean
}
