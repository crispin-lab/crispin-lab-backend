package com.crispinlab.composition.application.usecase.comment

import com.crispinlab.common.transaction.RecordingTransactionProvider
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.comment.CommentGettingComposition.Request
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.application.port.incoming.comment.CommentGetting
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

class CommentGettingCompositionUseCaseTest :
    DescribeSpec({
        val commentGetting = mockk<CommentGetting>()
        val userHandleLookup = mockk<UserHandleLookup>()

        fun useCaseWith(transactionProvider: TransactionProvider = RecordingTransactionProvider()) =
            CommentGettingCompositionUseCase(
                commentGetting = commentGetting,
                userHandleLookup = userHandleLookup,
                transactionProvider = transactionProvider
            )

        beforeEach { clearMocks(commentGetting, userHandleLookup) }

        describe("댓글 단건 조립") {
            it("도메인 Result 에 authorHandle 을 붙여 Result 로 반환한다") {
                every { commentGetting.perform(any()) } returns
                    domainResult(authorId = 100L, content = "안녕")
                every { userHandleLookup.handlesOf(setOf(UserId(100L))) } returns
                    mapOf(UserId(100L) to "alice")

                val result = useCaseWith().perform(basicRequest())

                result.authorHandle shouldBe "alice"
                result.content shouldBe "안녕"
            }

            it("lookup miss 이면 authorHandle 을 빈 문자열로 채운다") {
                every { commentGetting.perform(any()) } returns domainResult(authorId = 999L)
                every { userHandleLookup.handlesOf(any()) } returns emptyMap()

                val result = useCaseWith().perform(basicRequest())

                result.authorHandle shouldBe ""
            }

            it("Request 를 도메인 UseCase Request 로 그대로 넘긴다") {
                val requestSlot = slot<CommentGetting.Request>()
                every { commentGetting.perform(capture(requestSlot)) } returns
                    domainResult(authorId = 100L)
                every { userHandleLookup.handlesOf(any()) } returns
                    mapOf(UserId(100L) to "alice")

                useCaseWith().perform(
                    Request(pageId = "10", commentId = "7", viewer = MEMBER_VIEWER)
                )

                requestSlot.captured.pageId shouldBe PageId(10L)
                requestSlot.captured.commentId shouldBe CommentId(7L)
                requestSlot.captured.viewer shouldBe MEMBER_VIEWER
            }

            it("perform 진입에서 readOnly 트랜잭션으로 감싸고 lookup 은 tx 블록 안에서 호출한다 (LAB-156 회귀 방지)") {
                val transactionProvider = RecordingTransactionProvider()
                every { commentGetting.perform(any()) } returns domainResult(authorId = 100L)
                every { userHandleLookup.handlesOf(any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    mapOf(UserId(100L) to "alice")
                }

                useCaseWith(transactionProvider).perform(basicRequest())

                transactionProvider.readOnlyInvocations shouldBe listOf(true)
            }

            it("commentId 형식이 숫자가 아니면 IllegalArgumentException 을 그대로 올린다") {
                shouldThrow<IllegalArgumentException> {
                    useCaseWith().perform(
                        Request(
                            pageId = "10",
                            commentId = "not-a-number",
                            viewer = MEMBER_VIEWER
                        )
                    )
                }
            }
        }
    }) {
    companion object {
        val MEMBER_VIEWER: Viewer.Member = Viewer.Member(userId = UserId(100L), isAdmin = false)

        fun basicRequest(): Request =
            Request(pageId = "10", commentId = "7", viewer = MEMBER_VIEWER)

        fun domainResult(
            authorId: Long,
            content: String = "본문"
        ): CommentGetting.Result =
            CommentGetting.Result(
                commentId = CommentId(7L),
                pageId = PageId(10L),
                authorId = UserId(authorId),
                content = content,
                canEdit = false,
                createdAt = DUMMY_INSTANT,
                updatedAt = DUMMY_INSTANT
            )
    }
}
