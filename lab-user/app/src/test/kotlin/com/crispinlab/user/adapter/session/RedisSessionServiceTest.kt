package com.crispinlab.user.adapter.session

import com.crispinlab.user.domain.session.SessionToken
import com.crispinlab.user.testsupport.Fixtures.basicSessionToken
import com.crispinlab.user.testsupport.Fixtures.basicUser
import com.crispinlab.user.testsupport.RedisTestContext
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import java.time.Duration

class RedisSessionServiceTest :
    DescribeSpec({
        val redisTemplate = RedisTestContext.redisTemplate
        val sessionService = RedisSessionService(redisTemplate)

        afterEach { RedisTestContext.flushAll() }

        describe("RedisSessionService") {
            it("issue 한 토큰은 sess_ prefix + Base64Url 43자 형식이다") {
                val user = basicUser()

                val token = sessionService.issue(user.id)

                token.value shouldStartWith SessionToken.PREFIX
                token.value.removePrefix(SessionToken.PREFIX).length shouldBe
                    SessionToken.BODY_LENGTH
            }

            it("issue 직후 find 가 발급 대상 userId 를 반환한다") {
                val user = basicUser()
                val token = sessionService.issue(user.id)

                val found = sessionService.find(token)

                found.shouldNotBeNull()
                found.value shouldBe user.id.value
            }

            it("issue 후 TTL 이 24h 근처로 설정된다") {
                val user = basicUser()
                val token = sessionService.issue(user.id)

                val ttl = redisTemplate.getExpire("session:${token.value}")

                ttl shouldBeGreaterThan SLIDING_LOWER_BOUND
            }

            it("find 호출이 sliding 으로 TTL 을 재설정한다") {
                val user = basicUser()
                val token = sessionService.issue(user.id)
                redisTemplate.expire("session:${token.value}", Duration.ofSeconds(60))

                sessionService.find(token).shouldNotBeNull()
                val ttl = redisTemplate.getExpire("session:${token.value}")

                ttl shouldBeGreaterThan SLIDING_LOWER_BOUND
            }

            it("revoke 후 find 는 null 을 반환한다") {
                val user = basicUser()
                val token = sessionService.issue(user.id)

                sessionService.revoke(token)

                sessionService.find(token).shouldBeNull()
            }

            it("미존재 토큰의 revoke 는 예외 없이 멱등이다") {
                val unknown = basicSessionToken(body = "z".repeat(43))

                sessionService.revoke(unknown)
                sessionService.revoke(unknown)
            }

            it("issue 가 호출마다 다른 토큰을 발급한다") {
                val user = basicUser()

                val first = sessionService.issue(user.id)
                val second = sessionService.issue(user.id)

                first shouldNotBe second
            }
        }
    }) {
    companion object {
        private const val SLIDING_LOWER_BOUND: Long = 86_000L
    }
}
