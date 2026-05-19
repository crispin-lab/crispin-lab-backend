package com.crispinlab.user.application.usecase.auth

import com.crispinlab.user.application.port.incoming.auth.AuthLogout.Request
import com.crispinlab.user.application.port.outgoing.session.SessionService
import com.crispinlab.user.testsupport.Fixtures.basicSessionToken
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AuthLogoutUseCaseTest :
    DescribeSpec({
        val sessionService = mockk<SessionService>()
        val useCase = AuthLogoutUseCase(sessionService)

        beforeEach { clearMocks(sessionService) }

        describe("로그아웃") {
            it("정상 토큰을 revoke 한다") {
                val token = basicSessionToken()
                every { sessionService.revoke(any()) } returns Unit

                useCase.perform(Request(token = token.value))

                verify(exactly = 1) { sessionService.revoke(token) }
            }

            it("미존재 토큰도 revoke 호출까지만 위임한다 (어댑터가 멱등 보장)") {
                val token = basicSessionToken(body = "z".repeat(43))
                every { sessionService.revoke(any()) } returns Unit

                useCase.perform(Request(token = token.value))

                verify(exactly = 1) { sessionService.revoke(token) }
            }

            it("형식이 깨진 토큰은 revoke 를 호출하지 않고 멱등하게 무시한다") {
                useCase.perform(Request(token = "not-a-session-token"))

                verify(exactly = 0) { sessionService.revoke(any()) }
            }
        }
    })
