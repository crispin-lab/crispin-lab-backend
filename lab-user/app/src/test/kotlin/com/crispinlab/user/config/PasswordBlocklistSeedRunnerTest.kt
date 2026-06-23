package com.crispinlab.user.config

import com.crispinlab.user.adapter.credential.RedisPasswordBlocklistAdapter
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.springframework.boot.ApplicationArguments
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations

class PasswordBlocklistSeedRunnerTest :
    DescribeSpec({
        val redisTemplate = mockk<StringRedisTemplate>()
        val opsForValue = mockk<ValueOperations<String, String>>()
        val opsForSet = mockk<SetOperations<String, String>>()
        val runner = PasswordBlocklistSeedRunner(redisTemplate)
        val args = mockk<ApplicationArguments>(relaxed = true)

        beforeEach {
            clearMocks(redisTemplate, opsForValue, opsForSet)
            every { redisTemplate.opsForValue() } returns opsForValue
            every { redisTemplate.opsForSet() } returns opsForSet
        }

        describe("부팅 시 시드 주입") {
            it("MARKER 가 없으면 SADD 후 MARKER 를 설정한다") {
                every { redisTemplate.hasKey(MARKER_KEY) } returns false
                every { opsForSet.add(RedisPasswordBlocklistAdapter.KEY, *anyVararg()) } returns 20L
                every { opsForValue.set(MARKER_KEY, "true") } just Runs

                runner.run(args)

                verify(exactly = 1) {
                    opsForSet.add(RedisPasswordBlocklistAdapter.KEY, *anyVararg())
                }
                verify(exactly = 1) { opsForValue.set(MARKER_KEY, "true") }
            }

            it("MARKER 가 이미 있으면 SADD 가 호출되지 않는다") {
                every { redisTemplate.hasKey(MARKER_KEY) } returns true

                runner.run(args)

                verify(exactly = 0) {
                    opsForSet.add(any<String>(), *anyVararg())
                }
                verify(exactly = 0) { opsForValue.set(any(), any()) }
            }

            it("SADD 가 예외를 던지면 MARKER 가 설정되지 않는다 (다음 부팅 재시도 보장)") {
                every { redisTemplate.hasKey(MARKER_KEY) } returns false
                every { opsForSet.add(RedisPasswordBlocklistAdapter.KEY, *anyVararg()) } throws
                    RuntimeException("connection refused")

                runner.run(args)

                verify(exactly = 0) { opsForValue.set(any(), any()) }
            }

            it("hasKey 자체가 예외를 던져도 silent 진행") {
                every { redisTemplate.hasKey(MARKER_KEY) } throws
                    RuntimeException("connection refused")

                runner.run(args)

                verify(exactly = 0) { opsForSet.add(any<String>(), *anyVararg()) }
                verify(exactly = 0) { opsForValue.set(any(), any()) }
            }
        }
    }) {
    companion object {
        private const val MARKER_KEY: String = "password:blocklist:seeded:v1"
    }
}
