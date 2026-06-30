package com.crispinlab.user.application.usecase.user

import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.user.application.port.incoming.user.UserSearching.Request
import com.crispinlab.user.application.port.incoming.user.UserSearching.Request.Companion.DEFAULT_SIZE
import com.crispinlab.user.application.port.incoming.user.UserSearching.Result
import com.crispinlab.user.application.port.outgoing.user.UserSearchPort
import com.crispinlab.user.application.port.outgoing.user.UserSearchPort.Match
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify

class UserSearchingUseCaseTest :
    DescribeSpec({
        val userSearchPort = mockk<UserSearchPort>()
        val transactionProvider = spyk(DummyTransactionProvider())
        val useCase = UserSearchingUseCase(userSearchPort, transactionProvider)

        beforeEach { clearMocks(userSearchPort, transactionProvider) }

        describe("사용자 검색") {
            it("port 의 매칭 결과를 items 로 매핑한다") {
                every { userSearchPort.search(query = "ali", size = 10) } returns
                    listOf(
                        Match(userId = UserId(1L), handle = Handle("alice")),
                        Match(userId = UserId(2L), handle = Handle("alice_kim"))
                    )

                val result = useCase.perform(basicRequest(query = "ali"))

                result.items shouldBe
                    listOf(
                        Result.Item(userId = UserId(1L), handle = Handle("alice")),
                        Result.Item(userId = UserId(2L), handle = Handle("alice_kim"))
                    )
            }

            it("매칭이 없으면 items 가 빈 리스트다") {
                every { userSearchPort.search(any(), any()) } returns emptyList()

                useCase.perform(basicRequest(query = "zzz")).items.shouldBeEmpty()
            }

            it("Request 의 trim 된 query 와 size 가 port 인자로 전달된다") {
                val capturedQuery = slot<String>()
                val capturedSize = slot<Int>()
                every {
                    userSearchPort.search(
                        query = capture(capturedQuery),
                        size = capture(capturedSize)
                    )
                } returns emptyList()

                useCase.perform(basicRequest(query = "  bob ", size = 15))

                capturedQuery.captured shouldBe "bob"
                capturedSize.captured shouldBe 15
            }

            it("readOnly 트랜잭션 블록 안에서 조회된다") {
                every { userSearchPort.search(any(), any()) } returns emptyList()

                useCase.perform(basicRequest(query = "x"))

                verify(exactly = 1) {
                    transactionProvider.transactional<Result>(readOnly = true, block = any())
                }
            }

            it("query 가 빈 문자열이면 Request 생성 시점에 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(query = "")
                }
            }

            it("query 가 공백만 있으면 trim 후 빈 문자열이라 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(query = "   ")
                }
            }

            it("size 가 0 이면 Request 생성 시점에 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(query = "ok", size = 0)
                }
            }

            it("size 가 21 이상이면 Request 생성 시점에 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(query = "ok", size = 21)
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            query: String,
            size: Int = DEFAULT_SIZE
        ): Request = Request(query = query, size = size)
    }
}
