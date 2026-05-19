package com.crispinlab.user.application.usecase.auth

import com.crispinlab.common.exception.AuthenticationException
import com.crispinlab.user.application.port.incoming.auth.AuthLogin.Request
import com.crispinlab.user.application.port.outgoing.credential.PasswordEncoder
import com.crispinlab.user.application.port.outgoing.credential.UserCredentialRepository
import com.crispinlab.user.application.port.outgoing.session.SessionService
import com.crispinlab.user.application.port.outgoing.user.UserRepository
import com.crispinlab.user.domain.credential.Credential
import com.crispinlab.user.domain.credential.PasswordHash
import com.crispinlab.user.domain.user.UserErrorCode
import com.crispinlab.user.testsupport.DummyTransactionProvider
import com.crispinlab.user.testsupport.Fixtures.basicSessionToken
import com.crispinlab.user.testsupport.Fixtures.basicUser
import com.crispinlab.user.testsupport.Fixtures.basicUserCredential
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AuthLoginUseCaseTest :
    DescribeSpec({
        val userRepository = mockk<UserRepository>()
        val userCredentialRepository = mockk<UserCredentialRepository>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val sessionService = mockk<SessionService>()
        val useCase =
            AuthLoginUseCase(
                userRepository = userRepository,
                userCredentialRepository = userCredentialRepository,
                passwordEncoder = passwordEncoder,
                sessionService = sessionService,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(userRepository, userCredentialRepository, passwordEncoder, sessionService)
        }

        describe("로그인") {
            it("정상적으로 인증하고 세션 토큰을 발급한다") {
                val user = basicUser()
                val passwordHash = PasswordHash("\$2a\$12\$" + "a".repeat(53))
                every { userRepository.findByEmail(any()) } returns user
                every { userCredentialRepository.findPasswordBy(user.id) } returns
                    basicUserCredential(
                        userId = user.id,
                        credential = Credential.Password(passwordHash)
                    )
                every { passwordEncoder.matches("pass1234", passwordHash) } returns true
                val issuedToken = basicSessionToken(body = "b".repeat(43))
                every { sessionService.issue(user.id) } returns issuedToken

                val result = useCase.perform(basicRequest())

                result.userId shouldBe user.id
                result.token shouldBe issuedToken
            }

            it("이메일 형식이 올바르지 않으면 INVALID_CREDENTIALS 로 실패한다") {
                val exception =
                    shouldThrow<AuthenticationException> {
                        useCase.perform(basicRequest(email = "not-an-email"))
                    }
                exception.errorCode shouldBe UserErrorCode.INVALID_CREDENTIALS
                verify(exactly = 0) { sessionService.issue(any()) }
            }

            it("사용자가 없으면 INVALID_CREDENTIALS 로 실패한다") {
                every { userRepository.findByEmail(any()) } returns null

                val exception =
                    shouldThrow<AuthenticationException> { useCase.perform(basicRequest()) }
                exception.errorCode shouldBe UserErrorCode.INVALID_CREDENTIALS
                verify(exactly = 0) { sessionService.issue(any()) }
            }

            it("Password credential 이 없으면 INVALID_CREDENTIALS 로 실패한다") {
                val user = basicUser()
                every { userRepository.findByEmail(any()) } returns user
                every { userCredentialRepository.findPasswordBy(user.id) } returns null

                val exception =
                    shouldThrow<AuthenticationException> { useCase.perform(basicRequest()) }
                exception.errorCode shouldBe UserErrorCode.INVALID_CREDENTIALS
                verify(exactly = 0) { sessionService.issue(any()) }
            }

            it("비밀번호가 불일치하면 INVALID_CREDENTIALS 로 실패한다") {
                val user = basicUser()
                val passwordHash = PasswordHash("\$2a\$12\$" + "a".repeat(53))
                every { userRepository.findByEmail(any()) } returns user
                every { userCredentialRepository.findPasswordBy(user.id) } returns
                    basicUserCredential(
                        userId = user.id,
                        credential = Credential.Password(passwordHash)
                    )
                every { passwordEncoder.matches("wrong", passwordHash) } returns false

                val exception =
                    shouldThrow<AuthenticationException> {
                        useCase.perform(basicRequest(password = "wrong"))
                    }
                exception.errorCode shouldBe UserErrorCode.INVALID_CREDENTIALS
                verify(exactly = 0) { sessionService.issue(any()) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            email: String = "user@example.com",
            password: String = "pass1234"
        ): Request =
            Request(
                email = email,
                password = password
            )
    }
}
