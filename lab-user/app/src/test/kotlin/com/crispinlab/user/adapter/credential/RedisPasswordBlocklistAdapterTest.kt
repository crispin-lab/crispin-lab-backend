package com.crispinlab.user.adapter.credential

import com.crispinlab.user.adapter.credential.RedisPasswordBlocklistAdapter.Companion.KEY
import com.crispinlab.user.adapter.credential.RedisPasswordBlocklistAdapter.Companion.digestOf
import com.crispinlab.user.domain.credential.Password
import com.crispinlab.user.domain.credential.Password.Outcome.Ok
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.StringRedisTemplate

class RedisPasswordBlocklistAdapterTest :
    DescribeSpec({
        val redisTemplate = mockk<StringRedisTemplate>()
        val opsForSet = mockk<SetOperations<String, String>>()
        val adapter = RedisPasswordBlocklistAdapter(redisTemplate)

        beforeEach {
            clearMocks(redisTemplate, opsForSet)
            every { redisTemplate.opsForSet() } returns opsForSet
        }

        describe("isBlocked") {
            it("Redis 의 SISMEMBER 가 true 면 true 를 반환한다") {
                val password = password("Crispin!2026")
                every { opsForSet.isMember(KEY, digestOf(password.raw)) } returns true

                adapter.isBlocked(password) shouldBe true
            }

            it("Redis 의 SISMEMBER 가 false 면 false 를 반환한다") {
                val password = password("Crispin!2026")
                every { opsForSet.isMember(KEY, digestOf(password.raw)) } returns false

                adapter.isBlocked(password) shouldBe false
            }

            it("Redis 가 예외를 던지면 fail-open 으로 false 를 반환한다") {
                val password = password("Crispin!2026")
                every { opsForSet.isMember(KEY, any()) } throws
                    RuntimeException("connection refused")

                adapter.isBlocked(password) shouldBe false
            }

            it("isMember 가 한 번만 호출된다 (불필요한 round-trip 회피)") {
                val password = password("Crispin!2026")
                every { opsForSet.isMember(KEY, any()) } returns false

                adapter.isBlocked(password)

                verify(exactly = 1) { opsForSet.isMember(KEY, any()) }
            }
        }

        describe("digestOf") {
            it("case-insensitive 매칭 — 대소문자 차이 비밀번호는 같은 digest") {
                digestOf("Password") shouldBe digestOf("password")
                digestOf("PASSWORD") shouldBe digestOf("password")
            }

            it("다른 raw 는 다른 digest") {
                (digestOf("password") == digestOf("passw0rd")) shouldBe false
            }
        }
    })

private fun password(raw: String): Password = (Password.parse(raw) as Ok).password
