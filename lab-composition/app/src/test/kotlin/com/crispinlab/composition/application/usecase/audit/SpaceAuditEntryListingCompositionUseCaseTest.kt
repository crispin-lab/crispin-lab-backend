package com.crispinlab.composition.application.usecase.audit

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.RecordingTransactionProvider
import com.crispinlab.composition.application.port.incoming.audit.SpaceAuditEntryListingComposition.Request
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.application.port.incoming.audit.SpaceAuditEntryListing
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.audit.SpaceAuditAction
import com.crispinlab.space.domain.audit.SpaceAuditEntryId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class SpaceAuditEntryListingCompositionUseCaseTest :
    DescribeSpec({
        val spaceAuditEntryListing = mockk<SpaceAuditEntryListing>()
        val userHandleLookup = mockk<UserHandleLookup>()

        fun useCaseWith(
            transactionProvider: RecordingTransactionProvider = RecordingTransactionProvider()
        ) = SpaceAuditEntryListingCompositionUseCase(
            spaceAuditEntryListing = spaceAuditEntryListing,
            userHandleLookup = userHandleLookup,
            transactionProvider = transactionProvider
        )

        beforeEach { clearMocks(spaceAuditEntryListing, userHandleLookup) }

        describe("스페이스 감사 이력 조립") {
            it("도메인 결과 actorId 각각에 대해 handle 을 붙여 반환한다") {
                every { spaceAuditEntryListing.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                domainResult(
                                    id = 2L,
                                    actorUserId = 100L,
                                    action = SpaceAuditAction.EDITED
                                ),
                                domainResult(
                                    id = 1L,
                                    actorUserId = 200L,
                                    action = SpaceAuditAction.REGISTERED
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )
                every { userHandleLookup.handlesOf(any()) } returns
                    mapOf(UserId(100L) to "alice", UserId(200L) to "bob")

                val result = useCaseWith().perform(basicRequest())

                result.items.map { it.id to it.actorHandle } shouldBe
                    listOf(SpaceAuditEntryId(2L) to "alice", SpaceAuditEntryId(1L) to "bob")
            }

            it("distinct actorUserId 로 handlesOf 를 batch 1회 호출한다") {
                every { spaceAuditEntryListing.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                domainResult(id = 1L, actorUserId = 100L),
                                domainResult(id = 2L, actorUserId = 100L),
                                domainResult(id = 3L, actorUserId = 200L)
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

            it("lookup miss 는 actorHandle 을 빈 문자열로 채운다") {
                every { spaceAuditEntryListing.perform(any()) } returns
                    PageResult(
                        items = listOf(domainResult(id = 1L, actorUserId = 999L)),
                        page = 0,
                        size = 20,
                        totalElements = 1L
                    )
                every { userHandleLookup.handlesOf(any()) } returns emptyMap()

                val result = useCaseWith().perform(basicRequest())

                result.items.single().actorHandle shouldBe ""
            }

            it("handlesOf 가 예외로 실패해도 응답은 성공하고 actorHandle 은 빈 문자열로 대체된다") {
                every { spaceAuditEntryListing.perform(any()) } returns
                    PageResult(
                        items = listOf(domainResult(id = 1L, actorUserId = 100L)),
                        page = 0,
                        size = 20,
                        totalElements = 1L
                    )
                every { userHandleLookup.handlesOf(any()) } throws RuntimeException("boom")

                val result = useCaseWith().perform(basicRequest())

                result.items.single().actorHandle shouldBe ""
            }

            it("perform 은 readOnly 트랜잭션 안에서 실행된다") {
                val txProvider = RecordingTransactionProvider()
                every { spaceAuditEntryListing.perform(any()) } returns
                    PageResult(items = emptyList(), page = 0, size = 20, totalElements = 0L)
                every { userHandleLookup.handlesOf(any()) } returns emptyMap()

                useCaseWith(txProvider).perform(basicRequest())

                txProvider.readOnlyInvocations shouldBe listOf(true)
            }

            it("도메인 Request 로 변환해 perform 을 호출한다") {
                val captured = slot<SpaceAuditEntryListing.Request>()
                every { spaceAuditEntryListing.perform(capture(captured)) } returns
                    PageResult(items = emptyList(), page = 0, size = 20, totalElements = 0L)
                every { userHandleLookup.handlesOf(any()) } returns emptyMap()

                useCaseWith().perform(basicRequest(spaceId = "42", page = 2, size = 10))

                captured.captured.pageRequest.page shouldBe 2
                captured.captured.pageRequest.size shouldBe 10
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "1",
            page: Int = 0,
            size: Int = 20,
            userId: UserId = UserId(500L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                spaceId = spaceId,
                page = page,
                size = size,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )

        fun domainResult(
            id: Long,
            actorUserId: Long,
            action: SpaceAuditAction = SpaceAuditAction.EDITED,
            changeSummary: String = "{}"
        ): SpaceAuditEntryListing.Result =
            SpaceAuditEntryListing.Result(
                id = SpaceAuditEntryId(id),
                actorUserId = UserId(actorUserId),
                action = action,
                changeSummary = changeSummary,
                createdAt = DUMMY_INSTANT
            )
    }
}
