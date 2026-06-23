package com.crispinlab.user.config

import com.crispinlab.user.adapter.credential.RedisPasswordBlocklistAdapter
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
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
            it("첫 부팅 (SETNX 성공) 이면 SADD 가 호출된다") {
                every { opsForValue.setIfAbsent(MARKER_KEY, "true") } returns true
                every { opsForSet.add(RedisPasswordBlocklistAdapter.KEY, *anyVararg()) } returns 20L

                runner.run(args)

                verify(exactly = 1) {
                    opsForSet.add(RedisPasswordBlocklistAdapter.KEY, *anyVararg())
                }
            }

            it("두번째 부팅 (SETNX 실패) 이면 SADD 가 호출되지 않는다") {
                every { opsForValue.setIfAbsent(MARKER_KEY, "true") } returns false

                runner.run(args)

                verify(exactly = 0) {
                    opsForSet.add(any<String>(), *anyVararg())
                }
            }

            it("SADD 가 예외를 던지면 마커 cleanup 을 시도한다") {
                every { opsForValue.setIfAbsent(MARKER_KEY, "true") } returns true
                every { opsForSet.add(RedisPasswordBlocklistAdapter.KEY, *anyVararg()) } throws
                    RuntimeException("connection refused")
                every { redisTemplate.delete(MARKER_KEY) } returns true

                runner.run(args)

                verify(exactly = 1) { redisTemplate.delete(MARKER_KEY) }
            }

            it("SETNX 자체가 예외를 던져도 cleanup 시도 후 silent 진행") {
                every { opsForValue.setIfAbsent(MARKER_KEY, "true") } throws
                    RuntimeException("connection refused")
                every { redisTemplate.delete(MARKER_KEY) } returns true

                runner.run(args)

                verify(exactly = 1) { redisTemplate.delete(MARKER_KEY) }
                verify(exactly = 0) { opsForSet.add(any<String>(), *anyVararg()) }
            }
        }
    }) {
    companion object {
        private const val MARKER_KEY: String = "password:blocklist:seeded:v1"
    }
}
