package com.crispinlab.user.application.usecase.user

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.user.application.port.incoming.user.UserSigning.Request
import com.crispinlab.user.application.port.outgoing.credential.PasswordEncoder
import com.crispinlab.user.application.port.outgoing.credential.UserCredentialRepository
import com.crispinlab.user.application.port.outgoing.session.SessionService
import com.crispinlab.user.application.port.outgoing.user.UserRepository
import com.crispinlab.user.domain.credential.PasswordHash
import com.crispinlab.user.domain.credential.UserCredential
import com.crispinlab.user.domain.user.User
import com.crispinlab.user.domain.user.UserErrorCode
import com.crispinlab.user.testsupport.Fixtures.basicSessionToken
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UserSigningUseCaseTest :
    DescribeSpec({
        val userRepository = mockk<UserRepository>()
        val userCredentialRepository = mockk<UserCredentialRepository>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val sessionService = mockk<SessionService>()
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            UserSigningUseCase(
                userRepository = userRepository,
                userCredentialRepository = userCredentialRepository,
                passwordEncoder = passwordEncoder,
                sessionService = sessionService,
                idGenerator = idGenerator,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(
                userRepository,
                userCredentialRepository,
                passwordEncoder,
                sessionService,
                idGenerator
            )
        }

        describe("회원가입") {
            it("정상적으로 가입하고 세션 토큰을 발급한다") {
                every { userRepository.existsByEmail(any()) } returns false
                every { userRepository.existsByHandle(any()) } returns false
                every { idGenerator.next() } returnsMany listOf(100L, 200L)
                every { userRepository.save(any()) } answers { firstArg() }
                every { passwordEncoder.encode("pass1234") } returns
                    PasswordHash("\$2a\$12\$" + "a".repeat(53))
                every { userCredentialRepository.save(any()) } answers { firstArg() }
                val issuedToken = basicSessionToken()
                every { sessionService.issue(any()) } returns issuedToken

                val result = useCase.perform(basicRequest())

                result.userId.value shouldBe 100L
                result.token shouldBe issuedToken
                verify(exactly = 1) { userRepository.save(any<User>()) }
                verify(exactly = 1) { userCredentialRepository.save(any<UserCredential>()) }
            }

            it("이메일이 중복이면 EMAIL_DUPLICATED 로 실패한다") {
                every { userRepository.existsByEmail(any()) } returns true

                val exception = shouldThrow<ConflictException> { useCase.perform(basicRequest()) }
                exception.errorCode shouldBe UserErrorCode.EMAIL_DUPLICATED
                verify(exactly = 0) { sessionService.issue(any()) }
                verify(exactly = 0) { userRepository.save(any()) }
            }

            it("사용자 이름이 중복이면 HANDLE_DUPLICATED 로 실패한다") {
                every { userRepository.existsByEmail(any()) } returns false
                every { userRepository.existsByHandle(any()) } returns true

                val exception = shouldThrow<ConflictException> { useCase.perform(basicRequest()) }
                exception.errorCode shouldBe UserErrorCode.HANDLE_DUPLICATED
                verify(exactly = 0) { sessionService.issue(any()) }
                verify(exactly = 0) { userRepository.save(any()) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            email: String = "user@example.com",
            handle: String = "test_user",
            password: String = "pass1234"
        ): Request =
            Request(
                email = email,
                handle = handle,
                password = password
            )
    }
}
