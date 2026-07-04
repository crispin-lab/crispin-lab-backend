package com.crispinlab.composition.application.usecase.spacemember

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.RecordingTransactionProvider
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.spacemember.SpaceMemberListingComposition.Request
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberListing
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberListing.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class SpaceMemberListingCompositionUseCaseTest :
    DescribeSpec({
        val spaceMemberListing = mockk<SpaceMemberListing>()
        val userHandleLookup = mockk<UserHandleLookup>()

        fun useCaseWith(transactionProvider: TransactionProvider = RecordingTransactionProvider()) =
            SpaceMemberListingCompositionUseCase(
                spaceMemberListing = spaceMemberListing,
                userHandleLookup = userHandleLookup,
                transactionProvider = transactionProvider
            )

        beforeEach { clearMocks(spaceMemberListing, userHandleLookup) }

        describe("스페이스 멤버 목록 조립") {
            it("도메인 목록 결과에 handle 을 붙여 Result 로 반환한다") {
                every { spaceMemberListing.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                domainSummary(
                                    spaceMemberId = 1L,
                                    userId = 100L,
                                    role = SpaceMemberRole.OWNER
                                ),
                                domainSummary(
                                    spaceMemberId = 2L,
                                    userId = 200L,
                                    role = SpaceMemberRole.MEMBER
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )
                every { userHandleLookup.handlesOf(any()) } returns
                    mapOf(UserId(100L) to "alice", UserId(200L) to "bob")

                val result = useCaseWith().perform(basicRequest())

                result.items.map { it.spaceMemberId to it.handle } shouldBe
                    listOf(SpaceMemberId(1L) to "alice", SpaceMemberId(2L) to "bob")
            }

            it("lookup miss 인 userId 는 handle 을 빈 문자열로 채운다") {
                every { spaceMemberListing.perform(any()) } returns
                    PageResult(
                        items = listOf(domainSummary(spaceMemberId = 1L, userId = 999L)),
                        page = 0,
                        size = 20,
                        totalElements = 1L
                    )
                every { userHandleLookup.handlesOf(any()) } returns emptyMap()

                val result = useCaseWith().perform(basicRequest())

                result.items.single().handle shouldBe ""
            }

            it("distinct userIds 로 handlesOf 를 batch 1회 호출한다") {
                every { spaceMemberListing.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                domainSummary(spaceMemberId = 1L, userId = 100L),
                                domainSummary(spaceMemberId = 2L, userId = 100L),
                                domainSummary(spaceMemberId = 3L, userId = 200L)
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 3L
                    )
                every { userHandleLookup.handlesOf(any()) } returns
                    mapOf(UserId(100L) to "alice", UserId(200L) to "bob")

                useCaseWith().perform(basicRequest())

                verify(exactly = 1) {
                    userHandleLookup.handlesOf(setOf(UserId(100L), UserId(200L)))
                }
            }

            it("Request 를 도메인 UseCase Request 로 그대로 넘긴다") {
                val requestSlot = slot<SpaceMemberListing.Request>()
                every { spaceMemberListing.perform(capture(requestSlot)) } returns
                    PageResult(items = emptyList(), page = 0, size = 20, totalElements = 0L)
                every { userHandleLookup.handlesOf(any()) } returns emptyMap()

                useCaseWith().perform(
                    Request(
                        spaceId = "10",
                        page = 1,
                        size = 50,
                        viewer = Viewer.Anonymous
                    )
                )

                requestSlot.captured.spaceId shouldBe SpaceId(10L)
                requestSlot.captured.pageRequest.page shouldBe 1
                requestSlot.captured.pageRequest.size shouldBe 50
                requestSlot.captured.viewer shouldBe Viewer.Anonymous
            }

            it("perform 진입에서 readOnly 트랜잭션으로 감싸고 도메인 호출·lookup 모두 tx 블록 안에서 실행한다 (LAB-156 회귀 방지)") {
                val transactionProvider = RecordingTransactionProvider()
                every { spaceMemberListing.perform(any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    PageResult(items = emptyList(), page = 0, size = 20, totalElements = 0L)
                }
                every { userHandleLookup.handlesOf(any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    emptyMap()
                }

                useCaseWith(transactionProvider).perform(basicRequest())

                transactionProvider.readOnlyInvocations shouldBe listOf(true)
            }

            it("spaceId 형식이 숫자가 아니면 IllegalArgumentException 을 그대로 올린다") {
                shouldThrow<IllegalArgumentException> {
                    useCaseWith().perform(
                        Request(
                            spaceId = "abc",
                            page = 0,
                            size = 20,
                            viewer = Viewer.Anonymous
                        )
                    )
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(): Request =
            Request(
                spaceId = "10",
                page = 0,
                size = 20,
                viewer = Viewer.Anonymous
            )

        fun domainSummary(
            spaceMemberId: Long,
            userId: Long,
            role: SpaceMemberRole = SpaceMemberRole.MEMBER
        ): Summary =
            Summary(
                spaceMemberId = SpaceMemberId(spaceMemberId),
                spaceId = SpaceId(10L),
                userId = UserId(userId),
                role = role,
                joinedAt = DUMMY_INSTANT
            )
    }
}
