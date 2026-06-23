package com.crispinlab.user.config

import com.crispinlab.user.adapter.credential.RedisPasswordBlocklistAdapter
import com.crispinlab.user.adapter.credential.RedisPasswordBlocklistAdapter.Companion.digestOf
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class PasswordBlocklistSeedRunner(
    private val redisTemplate: StringRedisTemplate
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        runCatching {
            val firstTime = redisTemplate.opsForValue().setIfAbsent(MARKER_KEY, "true")
            if (firstTime != true) return
            redisTemplate
                .opsForSet()
                .add(RedisPasswordBlocklistAdapter.KEY, *SEED.map { digestOf(it) }.toTypedArray())
        }.onFailure { cause ->
            log.warn("blocklist seed 실패 type={}", cause.javaClass.simpleName)
            // 마커 cleanup 은 best-effort — 실패해도 다음 부팅에서 SETNX 가 자연 복구한다.
            runCatching { redisTemplate.delete(MARKER_KEY) }
        }
    }

    companion object {
        private const val MARKER_KEY: String = "password:blocklist:seeded:v1"
        private val SEED: List<String> =
            listOf(
                "password",
                "12345678",
                "qwerty",
                "admin",
                "letmein",
                "iloveyou",
                "welcome",
                "monkey",
                "dragon",
                "master",
                "sunshine",
                "princess",
                "abc12345",
                "password1",
                "11111111",
                "00000000",
                "87654321",
                "qwerty123",
                "passw0rd",
                "p@ssword"
            )
    }
}
