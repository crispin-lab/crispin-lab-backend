package com.crispinlab.user.adapter.security

import com.crispinlab.user.application.port.outgoing.credential.PasswordEncoder
import com.crispinlab.user.domain.credential.Password
import com.crispinlab.user.domain.credential.PasswordHash
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptPasswordEncoderAdapter(
    private val encoder: BCryptPasswordEncoder = BCryptPasswordEncoder(BCRYPT_COST)
) : PasswordEncoder {
    override fun encode(password: Password): PasswordHash =
        PasswordHash(encoder.encode(password.raw))

    override fun matches(
        raw: String,
        hash: PasswordHash
    ): Boolean = encoder.matches(raw, hash.value)

    companion object {
        private const val BCRYPT_COST: Int = 12
    }
}
