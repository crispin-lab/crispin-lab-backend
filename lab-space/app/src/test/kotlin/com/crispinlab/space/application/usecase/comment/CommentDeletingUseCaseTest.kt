package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentDeleting.Request
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.testsupport.Fixtures.basicComment
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class CommentDeletingUseCaseTest :
    DescribeSpec({
        val commentRepository = mockk<CommentRepository>()
        val useCase = CommentDeletingUseCase(commentRepository, DummyTransactionProvider())

        beforeEach {
            clearMocks(commentRepository)
        }

        describe("댓글 삭제") {
            it("commentRepository.delete 가 한 번 호출된다 (base 가 자동 soft delete 분기)") {
                val comment = basicComment(pageId = PageId(10L))
                every { commentRepository.findBy(comment.id) } returns comment
                justRun { commentRepository.delete(comment.id) }

                useCase.perform(
                    basicRequest(
                        pageId = "10",
                        commentId = comment.id.value.toString()
                    )
                )

                verify(exactly = 1) { commentRepository.delete(comment.id) }
                verify(exactly = 0) { commentRepository.save(any()) }
            }

            it("댓글이 없으면 NotFoundException") {
                every { commentRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { commentRepository.delete(any()) }
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
                verify(exactly = 0) { commentRepository.delete(any()) }
            }

            it("작성자가 아니면 NotFoundException 으로 응답한다") {
                val comment = basicComment(pageId = PageId(10L), authorId = UserId(200L))
                every { commentRepository.findBy(comment.id) } returns comment

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { commentRepository.delete(any()) }
            }

            it("ADMIN 은 작성자가 아니어도 삭제 가능하다") {
                val comment = basicComment(pageId = PageId(10L), authorId = UserId(200L))
                every { commentRepository.findBy(comment.id) } returns comment
                justRun { commentRepository.delete(comment.id) }

                useCase.perform(
                    basicRequest(
                        pageId = "10",
                        commentId = comment.id.value.toString(),
                        userId = UserId(100L),
                        isAdmin = true
                    )
                )

                verify(exactly = 1) { commentRepository.delete(comment.id) }
            }

            it("ADMIN 이라도 URL 의 pageId 와 댓글의 pageId 가 다르면 NotFoundException") {
                val comment = basicComment(pageId = PageId(999L))
                every { commentRepository.findBy(comment.id) } returns comment

                shouldThrow<NotFoundException> {
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            commentId = comment.id.value.toString(),
                            isAdmin = true
                        )
                    )
                }
                verify(exactly = 0) { commentRepository.delete(any()) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "10",
            commentId: String = "1",
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                pageId = pageId,
                commentId = commentId,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}
