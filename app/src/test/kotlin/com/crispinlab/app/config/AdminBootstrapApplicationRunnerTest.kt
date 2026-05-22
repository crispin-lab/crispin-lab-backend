package com.crispinlab.app.config

import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.user.application.port.outgoing.user.UserRepository
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.User
import com.crispinlab.user.testsupport.Fixtures.basicUser
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.boot.DefaultApplicationArguments

class AdminBootstrapApplicationRunnerTest :
    DescribeSpec({
        val userRepository = mockk<UserRepository>(relaxed = true)
        val transactionProvider =
            object : TransactionProvider {
                override fun <T> transactional(
                    readOnly: Boolean,
                    block: () -> T
                ): T = block()

                override fun afterRollback(block: () -> Unit) = Unit
            }

        fun runnerWith(adminEmail: String): AdminBootstrapApplicationRunner =
            AdminBootstrapApplicationRunner(
                adminEmail = adminEmail,
                userRepository = userRepository,
                transactionProvider = transactionProvider
            )

        beforeEach { clearMocks(userRepository) }

        describe("AdminBootstrapApplicationRunner") {
            it("admin.email 이 비어 있으면 아무 것도 하지 않는다") {
                runnerWith("").run(DefaultApplicationArguments())

                verify(exactly = 0) { userRepository.findByEmail(any()) }
            }

            it("admin.email 형식이 잘못되면 promote 를 건너뛴다") {
                runnerWith("not-an-email").run(DefaultApplicationArguments())

                verify(exactly = 0) { userRepository.findByEmail(any()) }
            }

            it("해당 이메일 사용자가 없으면 promote 를 건너뛴다") {
                every { userRepository.findByEmail(any()) } returns null

                runnerWith("admin@example.com").run(DefaultApplicationArguments())

                verify(exactly = 1) {
                    userRepository.findByEmail(EmailAddress("admin@example.com"))
                }
                verify(exactly = 0) { userRepository.save(any()) }
            }

            it("이미 ADMIN 인 사용자는 save 하지 않는다") {
                val admin = basicUser(role = SystemRole.ADMIN)
                every { userRepository.findByEmail(any()) } returns admin

                runnerWith("admin@example.com").run(DefaultApplicationArguments())

                verify(exactly = 0) { userRepository.save(any()) }
            }

            it("USER 역할 사용자를 ADMIN 으로 promote 하고 저장한다") {
                val user = basicUser(role = SystemRole.USER)
                every { userRepository.findByEmail(any()) } returns user
                val saved = slot<User>()
                every { userRepository.save(capture(saved)) } answers { saved.captured }

                runnerWith("admin@example.com").run(DefaultApplicationArguments())

                saved.captured.role shouldBe SystemRole.ADMIN
                verify(exactly = 1) { userRepository.save(user) }
            }
        }
    })
