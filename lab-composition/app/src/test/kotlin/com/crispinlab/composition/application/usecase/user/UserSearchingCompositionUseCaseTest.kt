package com.crispinlab.composition.application.usecase.user

import com.crispinlab.common.transaction.RecordingTransactionProvider
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.user.UserSearchingComposition.Request
import com.crispinlab.composition.application.port.outgoing.space.SpaceMembershipLookup
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.application.port.incoming.user.UserSearching
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class UserSearchingCompositionUseCaseTest :
    DescribeSpec({
        val userSearching = mockk<UserSearching>()
        val spaceMembershipLookup = mockk<SpaceMembershipLookup>()

        fun useCaseWith(transactionProvider: TransactionProvider = RecordingTransactionProvider()) =
            UserSearchingCompositionUseCase(
                userSearching = userSearching,
                spaceMembershipLookup = spaceMembershipLookup,
                transactionProvider = transactionProvider
            )

        beforeEach { clearMocks(userSearching, spaceMembershipLookup) }

        describe("사용자 검색 조립") {
            it("검색 결과에 소속 스페이스 집합을 붙여 Result 로 반환한다 (SpaceId 오름차순)") {
                every { userSearching.perform(any()) } returns
                    UserSearching.Result(
                        items =
                            listOf(
                                domainItem(userId = 1L, handle = "alice"),
                                domainItem(userId = 2L, handle = "alice_kim")
                            )
                    )
                every { spaceMembershipLookup.membershipsOf(any(), any()) } returns
                    mapOf(
                        UserId(1L) to setOf(SpaceId(20L), SpaceId(10L)),
                        UserId(2L) to setOf(SpaceId(10L))
                    )

                val result = useCaseWith().perform(basicRequest())

                result.items[0].userId shouldBe UserId(1L)
                result.items[0].memberOfSpaceIds shouldBe listOf(SpaceId(10L), SpaceId(20L))
                result.items[1].memberOfSpaceIds shouldBe listOf(SpaceId(10L))
            }

            it("distinct userIds 로 membershipsOf 를 batch 1회 호출한다") {
                every { userSearching.perform(any()) } returns
                    UserSearching.Result(
                        items =
                            listOf(
                                domainItem(userId = 1L, handle = "alice"),
                                domainItem(userId = 1L, handle = "alice"),
                                domainItem(userId = 2L, handle = "bob")
                            )
                    )
                every { spaceMembershipLookup.membershipsOf(any(), any()) } returns emptyMap()

                useCaseWith().perform(basicRequest())

                verify(exactly = 1) {
                    spaceMembershipLookup.membershipsOf(
                        userIds = setOf(UserId(1L), UserId(2L)),
                        viewer = MEMBER_VIEWER
                    )
                }
            }

            it("membershipsOf 가 예외를 던져도 items 는 반환하고 memberOfSpaceIds 는 빈 리스트로 응답한다") {
                every { userSearching.perform(any()) } returns
                    UserSearching.Result(
                        items = listOf(domainItem(userId = 1L, handle = "alice"))
                    )
                every { spaceMembershipLookup.membershipsOf(any(), any()) } throws
                    RuntimeException("lookup failure")

                val result = useCaseWith().perform(basicRequest())

                result.items.single().memberOfSpaceIds shouldBe emptyList()
            }

            it("Request 를 도메인 UseCase Request 로 그대로 넘긴다 (viewer 는 도메인 Request 에 미전달)") {
                val requestSlot = slot<UserSearching.Request>()
                every { userSearching.perform(capture(requestSlot)) } returns
                    UserSearching.Result(items = emptyList())
                every { spaceMembershipLookup.membershipsOf(any(), any()) } returns emptyMap()

                useCaseWith().perform(
                    Request(query = "alice", size = 15, viewer = MEMBER_VIEWER)
                )

                requestSlot.captured.query shouldBe "alice"
                requestSlot.captured.size shouldBe 15
            }

            it("Viewer.Member 가 membershipsOf 에 그대로 전달된다") {
                val viewerSlot = slot<Viewer.Member>()
                every { userSearching.perform(any()) } returns
                    UserSearching.Result(
                        items = listOf(domainItem(userId = 1L, handle = "alice"))
                    )
                every { spaceMembershipLookup.membershipsOf(any(), capture(viewerSlot)) } returns
                    emptyMap()

                useCaseWith().perform(
                    Request(
                        query = "alice",
                        size = 10,
                        viewer = Viewer.Member(userId = UserId(500L), isAdmin = true)
                    )
                )

                viewerSlot.captured.userId shouldBe UserId(500L)
                viewerSlot.captured.isAdmin shouldBe true
            }

            it("perform 진입에서 readOnly 트랜잭션으로 감싸고 도메인 호출·lookup 모두 tx 블록 안에서 실행한다 (LAB-156 회귀 방지)") {
                val transactionProvider = RecordingTransactionProvider()
                every { userSearching.perform(any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    UserSearching.Result(items = emptyList())
                }
                every { spaceMembershipLookup.membershipsOf(any(), any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    emptyMap()
                }

                useCaseWith(transactionProvider).perform(basicRequest())

                transactionProvider.readOnlyInvocations shouldBe listOf(true)
            }

            it("query 가 형식을 벗어나면 IllegalArgumentException 을 그대로 올린다") {
                shouldThrow<IllegalArgumentException> {
                    useCaseWith().perform(basicRequest(query = ""))
                }
            }
        }
    }) {
    companion object {
        val MEMBER_VIEWER: Viewer.Member = Viewer.Member(userId = UserId(100L), isAdmin = false)

        fun basicRequest(query: String = "alice"): Request =
            Request(
                query = query,
                size = 10,
                viewer = MEMBER_VIEWER
            )

        fun domainItem(
            userId: Long,
            handle: String
        ): UserSearching.Result.Item =
            UserSearching.Result.Item(
                userId = UserId(userId),
                handle = Handle(handle)
            )
    }
}
