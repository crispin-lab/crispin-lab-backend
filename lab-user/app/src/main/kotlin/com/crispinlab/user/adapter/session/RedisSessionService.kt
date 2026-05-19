package com.crispinlab.user.adapter.session

import com.crispinlab.user.application.port.outgoing.session.SessionService
import com.crispinlab.user.domain.session.SessionToken
import com.crispinlab.user.domain.user.UserId
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisSessionService(
    private val redisTemplate: StringRedisTemplate
) : SessionService {
    private val secureRandom = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    override fun issue(userId: UserId): SessionToken {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        val token = SessionToken(SessionToken.PREFIX + encoder.encodeToString(bytes))
        redisTemplate
            .opsForValue()
            .set(token.toKey(), userId.value.toString(), SESSION_TTL)
        return token
    }

    override fun find(token: SessionToken): UserId? {
        val key = token.toKey()
        val raw = redisTemplate.opsForValue().get(key) ?: return null
        redisTemplate.expire(key, SESSION_TTL)
        return runCatching { UserId(raw.toLong()) }
            .getOrElse { cause ->
                throw IllegalStateException("저장된 세션 값을 해석할 수 없습니다.", cause)
            }
    }

    override fun revoke(token: SessionToken) {
        redisTemplate.delete(token.toKey())
    }

    private fun SessionToken.toKey(): String = KEY_PREFIX + value

    companion object {
        private const val TOKEN_BYTES: Int = 32
        private const val KEY_PREFIX: String = "session:"
        private val SESSION_TTL: Duration = Duration.ofHours(24)
    }
}
