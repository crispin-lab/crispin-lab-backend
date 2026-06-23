package com.crispinlab.user.application.usecase.user

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.user.application.credential.PasswordPolicyException
import com.crispinlab.user.application.port.incoming.user.UserSigning.Request
import com.crispinlab.user.application.port.outgoing.credential.PasswordBlocklistPort
import com.crispinlab.user.application.port.outgoing.credential.PasswordEncoder
import com.crispinlab.user.application.port.outgoing.credential.UserCredentialRepository
import com.crispinlab.user.application.port.outgoing.session.SessionService
import com.crispinlab.user.application.port.outgoing.user.UserRepository
import com.crispinlab.user.domain.credential.PasswordErrorCode
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
        val passwordBlocklist = mockk<PasswordBlocklistPort>()
        val sessionService = mockk<SessionService>()
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            UserSigningUseCase(
                userRepository = userRepository,
                userCredentialRepository = userCredentialRepository,
                passwordEncoder = passwordEncoder,
                passwordBlocklist = passwordBlocklist,
                sessionService = sessionService,
                idGenerator = idGenerator,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(
                userRepository,
                userCredentialRepository,
                passwordEncoder,
                passwordBlocklist,
                sessionService,
                idGenerator
            )
            every { userRepository.existsByEmail(any()) } returns false
            every { userRepository.existsByHandle(any()) } returns false
            every { passwordBlocklist.isBlocked(any()) } returns false
        }

        describe("회원가입") {
            it("정상적으로 가입하고 세션 토큰을 발급한다") {
                every { idGenerator.next() } returnsMany listOf(100L, 200L)
                every { userRepository.save(any()) } answers { firstArg() }
                every { passwordEncoder.encode(match { it.raw == SAFE_PASSWORD }) } returns
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
                every { userRepository.existsByHandle(any()) } returns true

                val exception = shouldThrow<ConflictException> { useCase.perform(basicRequest()) }
                exception.errorCode shouldBe UserErrorCode.HANDLE_DUPLICATED
                verify(exactly = 0) { sessionService.issue(any()) }
                verify(exactly = 0) { userRepository.save(any()) }
            }

            it("비밀번호가 이메일 local-part 를 포함하면 SIMILAR_TO_IDENTITY 로 실패한다") {
                val exception =
                    shouldThrow<PasswordPolicyException> {
                        useCase.perform(
                            basicRequest(
                                email = "$SIMILAR_LOCAL@example.com",
                                password = "$SIMILAR_LOCAL$SAFE_PASSWORD"
                            )
                        )
                    }
                exception.errorCode shouldBe PasswordErrorCode.PASSWORD_SIMILAR_TO_IDENTITY
                verify(exactly = 0) { userRepository.save(any()) }
            }

            it("비밀번호가 사용자 이름을 포함하면 SIMILAR_TO_IDENTITY 로 실패한다") {
                val exception =
                    shouldThrow<PasswordPolicyException> {
                        useCase.perform(
                            basicRequest(
                                handle = SIMILAR_HANDLE,
                                password = "$SAFE_PASSWORD$SIMILAR_HANDLE"
                            )
                        )
                    }
                exception.errorCode shouldBe PasswordErrorCode.PASSWORD_SIMILAR_TO_IDENTITY
                verify(exactly = 0) { userRepository.save(any()) }
            }

            it("짧은 handle (3자) substring 매칭은 광범위 false positive 를 일으키지 않는다") {
                every { idGenerator.next() } returnsMany listOf(100L, 200L)
                every { userRepository.save(any()) } answers { firstArg() }
                every { passwordEncoder.encode(any()) } returns
                    PasswordHash("\$2a\$12\$" + "a".repeat(53))
                every { userCredentialRepository.save(any()) } answers { firstArg() }
                every { sessionService.issue(any()) } returns basicSessionToken()

                useCase.perform(basicRequest(handle = "abc", password = "abcXyz!12"))

                verify(exactly = 1) { userRepository.save(any<User>()) }
            }

            it("blocklist 에 적중하면 BLOCKED 로 실패한다") {
                every { passwordBlocklist.isBlocked(any()) } returns true

                val exception =
                    shouldThrow<PasswordPolicyException> { useCase.perform(basicRequest()) }
                exception.errorCode shouldBe PasswordErrorCode.PASSWORD_BLOCKED
                verify(exactly = 0) { userRepository.save(any()) }
            }
        }
    }) {
    companion object {
        const val SAFE_PASSWORD: String = "Crispin!2026"
        private const val SIMILAR_LOCAL: String = "alicia"
        private const val SIMILAR_HANDLE: String = "test_user"

        fun basicRequest(
            email: String = "user@example.com",
            handle: String = "neutral_handle",
            password: String = SAFE_PASSWORD
        ): Request =
            Request(
                email = email,
                handle = handle,
                password = password
            )
    }
}
