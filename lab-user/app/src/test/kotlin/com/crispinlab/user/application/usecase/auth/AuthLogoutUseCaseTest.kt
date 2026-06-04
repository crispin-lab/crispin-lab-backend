package com.crispinlab.user.application.usecase.auth

import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.user.application.port.incoming.auth.AuthLogout.Request
import com.crispinlab.user.application.port.outgoing.session.SessionService
import com.crispinlab.user.testsupport.Fixtures.basicSessionToken
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder

class AuthLogoutUseCaseTest :
    DescribeSpec({
        val sessionService = mockk<SessionService>()
        val transactionProvider = spyk(DummyTransactionProvider())
        val useCase = AuthLogoutUseCase(sessionService, transactionProvider)

        beforeEach { clearMocks(sessionService, transactionProvider) }

        describe("로그아웃") {
            it("정상 토큰을 revoke 한다") {
                val token = basicSessionToken()
                every { sessionService.revoke(any()) } returns Unit

                useCase.perform(Request(token = token))

                verify(exactly = 1) { sessionService.revoke(token) }
            }

            it("미존재 토큰도 revoke 호출까지만 위임한다 (어댑터가 멱등 보장)") {
                val token = basicSessionToken(body = "z".repeat(43))
                every { sessionService.revoke(any()) } returns Unit

                useCase.perform(Request(token = token))

                verify(exactly = 1) { sessionService.revoke(token) }
            }

            it("revoke 호출은 transactional 블록 안에서 실행된다") {
                val token = basicSessionToken()
                var inTransaction = false
                every {
                    transactionProvider.transactional<Unit>(readOnly = false, block = any())
                } answers {
                    inTransaction = true
                    try {
                        callOriginal()
                    } finally {
                        inTransaction = false
                    }
                }
                every { sessionService.revoke(any()) } answers {
                    check(inTransaction) {
                        "sessionService.revoke 는 트랜잭션 안에서만 호출되어야 한다."
                    }
                    Unit
                }

                useCase.perform(Request(token = token))

                verify(exactly = 1) { sessionService.revoke(token) }
            }
        }
    })
