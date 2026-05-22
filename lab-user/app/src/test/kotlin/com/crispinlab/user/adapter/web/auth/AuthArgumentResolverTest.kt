package com.crispinlab.user.adapter.web.auth

import com.crispinlab.common.exception.AuthenticationException
import com.crispinlab.user.application.port.outgoing.session.SessionService
import com.crispinlab.user.application.port.outgoing.user.UserRepository
import com.crispinlab.user.domain.session.SessionErrorCode
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.Fixtures.basicSessionToken
import com.crispinlab.user.testsupport.Fixtures.basicUser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest

class AuthArgumentResolverTest :
    DescribeSpec({
        val sessionService = mockk<SessionService>()
        val userRepository = mockk<UserRepository>()
        val resolver = AuthArgumentResolver(sessionService, userRepository)

        beforeEach { clearMocks(sessionService, userRepository) }

        describe("AuthArgumentResolver") {
            it("Auth 파라미터를 처리할 수 있다고 보고한다") {
                resolver.supportsParameter(parameterOfType(Auth::class.java)) shouldBe true
            }

            it("Auth 가 아닌 파라미터는 처리하지 않는다") {
                resolver.supportsParameter(parameterOfType(String::class.java)) shouldBe false
            }

            it("정상 토큰을 받으면 Auth 로 변환한다") {
                val token = basicSessionToken()
                val user = basicUser(id = UserId(100L), role = SystemRole.USER)
                every { sessionService.find(token) } returns user.id
                every { userRepository.findBy(user.id) } returns user

                val auth =
                    resolver.resolveArgument(
                        request = bearer(token.value)
                    )

                auth.userId shouldBe UserId(100L)
                auth.role shouldBe SystemRole.USER
                auth.isAdmin shouldBe false
                verifyOrder {
                    sessionService.find(token)
                    userRepository.findBy(user.id)
                }
            }

            it("ADMIN 역할 사용자도 동일하게 변환한다") {
                val token = basicSessionToken()
                val user = basicUser(id = UserId(7L), role = SystemRole.ADMIN)
                every { sessionService.find(token) } returns user.id
                every { userRepository.findBy(user.id) } returns user

                val auth =
                    resolver.resolveArgument(
                        request = bearer(token.value)
                    )

                auth.role shouldBe SystemRole.ADMIN
                auth.isAdmin shouldBe true
            }

            it("Authorization 헤더가 없으면 INVALID_SESSION 으로 401") {
                shouldThrowInvalidSession {
                    resolver.resolveArgument(
                        request = ServletWebRequest(MockHttpServletRequest())
                    )
                }
                verify(exactly = 0) { sessionService.find(any()) }
            }

            it("Bearer prefix 가 없으면 INVALID_SESSION 으로 401") {
                shouldThrowInvalidSession {
                    resolver.resolveArgument(
                        request = withHeader("just-a-raw-token")
                    )
                }
                verify(exactly = 0) { sessionService.find(any()) }
            }

            it("Bearer 뒤 토큰이 비어 있으면 INVALID_SESSION 으로 401") {
                shouldThrowInvalidSession {
                    resolver.resolveArgument(
                        request = withHeader("Bearer ")
                    )
                }
                verify(exactly = 0) { sessionService.find(any()) }
            }

            it("토큰 형식이 SessionToken 규약에 맞지 않으면 INVALID_SESSION 으로 401") {
                shouldThrowInvalidSession {
                    resolver.resolveArgument(
                        request = bearer("not-a-sess-token")
                    )
                }
                verify(exactly = 0) { sessionService.find(any()) }
            }

            it("세션이 만료/삭제되어 SessionService.find 가 null 이면 INVALID_SESSION 으로 401") {
                val token = basicSessionToken()
                every { sessionService.find(token) } returns null

                shouldThrowInvalidSession {
                    resolver.resolveArgument(
                        request = bearer(token.value)
                    )
                }
                verify(exactly = 0) { userRepository.findBy(any()) }
            }

            it("세션은 있어도 사용자가 삭제되었으면 INVALID_SESSION 으로 401") {
                val token = basicSessionToken()
                val userId = UserId(42L)
                every { sessionService.find(token) } returns userId
                every { userRepository.findBy(userId) } returns null

                shouldThrowInvalidSession {
                    resolver.resolveArgument(
                        request = bearer(token.value)
                    )
                }
            }

            it("옵셔널 파라미터에 헤더가 없으면 401 대신 null 을 돌려준다") {
                resolver
                    .resolveOptionalArgument(
                        request = ServletWebRequest(MockHttpServletRequest())
                    ).shouldBeNull()
                verify(exactly = 0) { sessionService.find(any()) }
            }

            it("옵셔널 파라미터라도 만료/잘못된 토큰이 오면 401 을 던진다") {
                val token = basicSessionToken()
                every { sessionService.find(token) } returns null

                shouldThrowInvalidSession {
                    resolver.resolveOptionalArgument(
                        request = bearer(token.value)
                    )
                }
            }

            it("옵셔널 파라미터라도 정상 토큰이면 Auth 를 돌려준다") {
                val token = basicSessionToken()
                val user = basicUser(id = UserId(100L), role = SystemRole.USER)
                every { sessionService.find(token) } returns user.id
                every { userRepository.findBy(user.id) } returns user

                val auth =
                    resolver.resolveOptionalArgument(
                        request = bearer(token.value)
                    )

                auth.shouldNotBeNull()
                auth.userId shouldBe UserId(100L)
                auth.role shouldBe SystemRole.USER
            }
        }
    }) {
    companion object {
        private fun parameterOfType(type: Class<*>): MethodParameter =
            mockk<MethodParameter>().also {
                every { it.parameterType } returns type
            }

        private fun bearer(raw: String) = withHeader("Bearer $raw")

        private fun withHeader(value: String): ServletWebRequest =
            ServletWebRequest(
                MockHttpServletRequest().apply { addHeader(AUTHORIZATION, value) }
            )

        private fun AuthArgumentResolver.resolveArgument(request: ServletWebRequest): Auth =
            resolveArgument(
                parameter = parameterMock(optional = false),
                mavContainer = null,
                webRequest = request,
                binderFactory = null
            )!!

        private fun AuthArgumentResolver.resolveOptionalArgument(
            request: ServletWebRequest
        ): Auth? =
            resolveArgument(
                parameter = parameterMock(optional = true),
                mavContainer = null,
                webRequest = request,
                binderFactory = null
            )

        private fun parameterMock(optional: Boolean): MethodParameter =
            mockk(relaxed = true) {
                every { isOptional } returns optional
            }

        private fun shouldThrowInvalidSession(block: () -> Unit) {
            val ex = shouldThrow<AuthenticationException>(block)
            ex.errorCode.code shouldBe SessionErrorCode.INVALID_SESSION.code
        }
    }
}
