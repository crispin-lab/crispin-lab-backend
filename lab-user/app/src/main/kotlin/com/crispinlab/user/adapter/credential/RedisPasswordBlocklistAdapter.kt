package com.crispinlab.user.adapter.credential

import com.crispinlab.user.application.port.outgoing.credential.PasswordBlocklistPort
import com.crispinlab.user.domain.credential.Password
import java.security.MessageDigest
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisPasswordBlocklistAdapter(
    private val redisTemplate: StringRedisTemplate
) : PasswordBlocklistPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun isBlocked(password: Password): Boolean =
        runCatching {
            redisTemplate.opsForSet().isMember(KEY, digestOf(password.raw)) == true
        }.getOrElse { cause ->
            log.warn("blocklist lookup 실패 type={}", cause.javaClass.simpleName)
            false
        }

    companion object {
        const val KEY: String = "password:blocklist"

        /**
         * 정책: case-insensitive 매칭. raw 를 lowercase 한 뒤 SHA-256.
         * SEED / 운영자 ingest 가 같은 규약을 따라야 hit 가 일관.
         */
        internal fun digestOf(raw: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(raw.lowercase().toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
