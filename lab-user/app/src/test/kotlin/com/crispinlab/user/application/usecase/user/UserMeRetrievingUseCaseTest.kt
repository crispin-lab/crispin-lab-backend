package com.crispinlab.user.application.usecase.user

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.user.application.port.incoming.user.UserMeRetrieving.Request
import com.crispinlab.user.application.port.incoming.user.UserMeRetrieving.Result
import com.crispinlab.user.application.port.outgoing.user.UserRepository
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.Fixtures.basicUser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify

class UserMeRetrievingUseCaseTest :
    DescribeSpec({
        val userRepository = mockk<UserRepository>()
        val transactionProvider = spyk(DummyTransactionProvider())
        val useCase = UserMeRetrievingUseCase(userRepository, transactionProvider)

        beforeEach { clearMocks(userRepository, transactionProvider) }

        describe("현재 세션 사용자 조회") {
            it("정상적으로 자기 정보를 반환한다") {
                val user =
                    basicUser(
                        id = UserId(42L),
                        handle = Handle("alice_kim"),
                        email = EmailAddress("alice@example.com"),
                        role = SystemRole.USER
                    )
                every { userRepository.findBy(user.id) } returns user

                val result = useCase.perform(Request(currentUserId = "42"))

                result.userId shouldBe user.id
                result.handle shouldBe user.handle
                result.email shouldBe user.email
                result.isAdmin shouldBe false
            }

            it("ADMIN 사용자면 isAdmin 이 true 다") {
                val user = basicUser(role = SystemRole.ADMIN)
                every { userRepository.findBy(user.id) } returns user

                val result = useCase.perform(Request(currentUserId = user.id.value.toString()))

                result.isAdmin shouldBe true
            }

            it("사용자가 없으면 NotFoundException 을 던진다") {
                every { userRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(Request(currentUserId = "42"))
                }
            }

            it("readOnly 트랜잭션 블록 안에서 조회된다") {
                val user = basicUser()
                every { userRepository.findBy(user.id) } returns user

                useCase.perform(Request(currentUserId = user.id.value.toString()))

                verify(exactly = 1) {
                    transactionProvider.transactional<Result>(readOnly = true, block = any())
                }
            }
        }
    })
