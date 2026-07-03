package com.crispinlab.composition.application.usecase.comment

import com.crispinlab.common.transaction.RecordingTransactionProvider
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.comment.CommentRegisteringComposition.Request
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

class CommentRegisteringCompositionUseCaseTest :
    DescribeSpec({
        val commentRegistering = mockk<CommentRegistering>()
        val userHandleLookup = mockk<UserHandleLookup>()

        fun useCaseWith(transactionProvider: TransactionProvider = RecordingTransactionProvider()) =
            CommentRegisteringCompositionUseCase(
                commentRegistering = commentRegistering,
                userHandleLookup = userHandleLookup,
                transactionProvider = transactionProvider
            )

        beforeEach { clearMocks(commentRegistering, userHandleLookup) }

        describe("댓글 등록 조립") {
            it("도메인 Result 에 authorHandle 을 붙여 Result 로 반환한다") {
                every { commentRegistering.perform(any()) } returns
                    CommentRegistering.Result(
                        commentId = CommentId(42L),
                        authorId = UserId(100L)
                    )
                every { userHandleLookup.handlesOf(setOf(UserId(100L))) } returns
                    mapOf(UserId(100L) to "alice")

                val result = useCaseWith().perform(basicRequest())

                result.commentId shouldBe CommentId(42L)
                result.authorHandle shouldBe "alice"
            }

            it("handle 조회가 실패해도 쓰기 성공 응답을 반환한다 (authorHandle 빈 문자열)") {
                every { commentRegistering.perform(any()) } returns
                    CommentRegistering.Result(
                        commentId = CommentId(42L),
                        authorId = UserId(100L)
                    )
                every { userHandleLookup.handlesOf(any()) } throws
                    RuntimeException("lookup failure")

                val result = useCaseWith().perform(basicRequest())

                result.commentId shouldBe CommentId(42L)
                result.authorHandle shouldBe ""
            }

            it("Request 를 도메인 UseCase Request 로 그대로 넘긴다") {
                val requestSlot = slot<CommentRegistering.Request>()
                every { commentRegistering.perform(capture(requestSlot)) } returns
                    CommentRegistering.Result(
                        commentId = CommentId(42L),
                        authorId = UserId(100L)
                    )
                every { userHandleLookup.handlesOf(any()) } returns
                    mapOf(UserId(100L) to "alice")

                useCaseWith().perform(
                    Request(pageId = "10", content = "첫 댓글", viewer = MEMBER_VIEWER)
                )

                requestSlot.captured.pageId.value shouldBe 10L
                requestSlot.captured.content.raw shouldBe "첫 댓글"
                requestSlot.captured.viewer shouldBe MEMBER_VIEWER
            }

            it("도메인 write 는 tx 밖에서 완결되고 lookup 만 readOnly tx 안에서 호출한다 (LAB-156 회귀 방지)") {
                val transactionProvider = RecordingTransactionProvider()
                every { commentRegistering.perform(any()) } answers {
                    transactionProvider.inTransaction shouldBe false
                    CommentRegistering.Result(
                        commentId = CommentId(42L),
                        authorId = UserId(100L)
                    )
                }
                every { userHandleLookup.handlesOf(any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    mapOf(UserId(100L) to "alice")
                }

                useCaseWith(transactionProvider).perform(basicRequest())

                transactionProvider.readOnlyInvocations shouldBe listOf(true)
            }

            it("pageId 형식이 숫자가 아니면 IllegalArgumentException 을 그대로 올린다") {
                shouldThrow<IllegalArgumentException> {
                    useCaseWith().perform(
                        Request(
                            pageId = "not-a-number",
                            content = "첫 댓글",
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
            Request(pageId = "10", content = "첫 댓글", viewer = MEMBER_VIEWER)
    }
}
