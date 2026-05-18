package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Request
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicComment
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class CommentEditingUseCaseTest :
    DescribeSpec({
        val commentRepository = mockk<CommentRepository>()
        val useCase = CommentEditingUseCase(commentRepository, DummyTransactionProvider())

        beforeEach {
            clearMocks(commentRepository)
            every { commentRepository.save(any()) } answers { firstArg() }
        }

        describe("댓글 수정") {
            it("body 를 갱신하고 Result 를 반환한다") {
                val comment = basicComment(pageId = PageId(10L), body = "이전")
                every { commentRepository.findBy(comment.id) } returns comment
                val saved = slot<Comment>()
                every { commentRepository.save(capture(saved)) } answers { saved.captured }

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            commentId = comment.id.value.toString(),
                            body = "수정됨"
                        )
                    )

                result.body shouldBe "수정됨"
                saved.captured.body shouldBe "수정됨"
            }

            it("댓글이 없으면 NotFoundException") {
                every { commentRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { commentRepository.save(any()) }
            }

            it("URL 의 pageId 와 댓글의 pageId 가 다르면 NotFoundException") {
                val comment = basicComment(pageId = PageId(999L))
                every { commentRepository.findBy(comment.id) } returns comment

                shouldThrow<NotFoundException> {
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            commentId = comment.id.value.toString()
                        )
                    )
                }
                verify(exactly = 0) { commentRepository.save(any()) }
            }

            it("작성자가 아니면 NotFoundException (존재/권한 응답 통합)") {
                val comment = basicComment(pageId = PageId(10L), authorId = UserId(200L))
                every { commentRepository.findBy(comment.id) } returns comment

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(currentUserId = UserId(100L)))
                }
                verify(exactly = 0) { commentRepository.save(any()) }
            }

            it("이미 삭제된 댓글은 자동 필터로 findBy 가 null 을 반환하므로 NotFoundException") {
                every { commentRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { commentRepository.save(any()) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "10",
            commentId: String = "1",
            body: String = "수정된 댓글",
            currentUserId: UserId = UserId(100L)
        ): Request =
            Request(
                pageId = pageId,
                commentId = commentId,
                body = body,
                currentUserId = currentUserId
            )
    }
}
