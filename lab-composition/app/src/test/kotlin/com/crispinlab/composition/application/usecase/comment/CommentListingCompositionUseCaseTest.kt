package com.crispinlab.composition.application.usecase.comment

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.RecordingTransactionProvider
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.comment.CommentListingComposition.Request
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.application.port.incoming.comment.CommentListing
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
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

class CommentListingCompositionUseCaseTest :
    DescribeSpec({
        val commentListing = mockk<CommentListing>()
        val userHandleLookup = mockk<UserHandleLookup>()

        fun useCaseWith(transactionProvider: TransactionProvider = RecordingTransactionProvider()) =
            CommentListingCompositionUseCase(
                commentListing = commentListing,
                userHandleLookup = userHandleLookup,
                transactionProvider = transactionProvider
            )

        beforeEach { clearMocks(commentListing, userHandleLookup) }

        describe("댓글 목록 조립") {
            it("도메인 결과에 authorHandle 을 붙여 Result 로 반환한다") {
                every { commentListing.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                domainSummary(commentId = 1L, authorId = 100L, content = "첫 댓글"),
                                domainSummary(commentId = 2L, authorId = 101L, content = "두 번째")
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )
                every { userHandleLookup.handlesOf(any()) } returns
                    mapOf(UserId(100L) to "alice", UserId(101L) to "bob")

                val result = useCaseWith().perform(basicRequest())

                result.items.map { it.authorHandle } shouldBe listOf("alice", "bob")
            }

            it("distinct authorIds 로 handlesOf 를 batch 1회 호출한다") {
                every { commentListing.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                domainSummary(commentId = 1L, authorId = 100L),
                                domainSummary(commentId = 2L, authorId = 100L),
                                domainSummary(commentId = 3L, authorId = 101L)
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 3L
                    )
                every { userHandleLookup.handlesOf(any()) } returns
                    mapOf(UserId(100L) to "alice", UserId(101L) to "bob")

                useCaseWith().perform(basicRequest())

                verify(exactly = 1) {
                    userHandleLookup.handlesOf(setOf(UserId(100L), UserId(101L)))
                }
            }

            it("lookup miss 이면 authorHandle 을 빈 문자열로 채운다") {
                every { commentListing.perform(any()) } returns
                    PageResult(
                        items = listOf(domainSummary(commentId = 1L, authorId = 999L)),
                        page = 0,
                        size = 20,
                        totalElements = 1L
                    )
                every { userHandleLookup.handlesOf(any()) } returns emptyMap()

                val result = useCaseWith().perform(basicRequest())

                result.items.single().authorHandle shouldBe ""
            }

            it("Request 를 도메인 UseCase Request 로 그대로 넘긴다") {
                val requestSlot = slot<CommentListing.Request>()
                every { commentListing.perform(capture(requestSlot)) } returns
                    PageResult(items = emptyList(), page = 0, size = 20, totalElements = 0L)
                every { userHandleLookup.handlesOf(any()) } returns emptyMap()

                useCaseWith().perform(
                    Request(pageId = "10", page = 1, size = 50, viewer = MEMBER_VIEWER)
                )

                requestSlot.captured.pageId shouldBe PageId(10L)
                requestSlot.captured.pageRequest.page shouldBe 1
                requestSlot.captured.pageRequest.size shouldBe 50
                requestSlot.captured.viewer shouldBe MEMBER_VIEWER
            }

            it("perform 진입에서 readOnly 트랜잭션으로 감싸고 lookup 은 tx 블록 안에서 호출한다 (LAB-156 회귀 방지)") {
                val transactionProvider = RecordingTransactionProvider()
                every { commentListing.perform(any()) } returns
                    PageResult(items = emptyList(), page = 0, size = 20, totalElements = 0L)
                every { userHandleLookup.handlesOf(any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    emptyMap()
                }

                useCaseWith(transactionProvider).perform(basicRequest())

                transactionProvider.readOnlyInvocations shouldBe listOf(true)
            }

            it("pageId 형식이 숫자가 아니면 IllegalArgumentException 을 그대로 올린다") {
                shouldThrow<IllegalArgumentException> {
                    useCaseWith().perform(basicRequest(pageId = "not-a-number"))
                }
            }
        }
    }) {
    companion object {
        val MEMBER_VIEWER: Viewer.Member = Viewer.Member(userId = UserId(100L), isAdmin = false)

        fun basicRequest(pageId: String = "10"): Request =
            Request(
                pageId = pageId,
                page = 0,
                size = 20,
                viewer = MEMBER_VIEWER
            )

        fun domainSummary(
            commentId: Long,
            authorId: Long,
            content: String = "본문",
            canEdit: Boolean = false
        ): Summary =
            Summary(
                commentId = CommentId(commentId),
                pageId = PageId(10L),
                authorId = UserId(authorId),
                content = content,
                canEdit = canEdit,
                createdAt = DUMMY_INSTANT,
                updatedAt = DUMMY_INSTANT
            )
    }
}
