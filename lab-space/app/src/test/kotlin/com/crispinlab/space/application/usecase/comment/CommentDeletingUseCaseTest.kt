package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.comment.CommentDeleting.Request
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicComment
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CommentDeletingUseCaseTest :
    DescribeSpec({
        val commentRepository = mockk<CommentRepository>()
        val useCase = CommentDeletingUseCase(commentRepository, DummyTransactionProvider())

        beforeEach {
            clearMocks(commentRepository)
            every { commentRepository.save(any()) } answers { firstArg() }
        }

        describe("댓글 삭제") {
            it("soft delete — isDeleted=true, deletedAt 비어있지 않은 상태로 save 가 호출된다") {
                val comment = basicComment(pageId = PageId(10L))
                comment.isDeleted shouldBe false
                every { commentRepository.findBy(comment.id) } returns comment

                useCase.perform(
                    basicRequest(
                        pageId = "10",
                        commentId = comment.id.value.toString()
                    )
                )

                verify(exactly = 1) {
                    commentRepository.save(
                        match { saved ->
                            saved.isDeleted && saved.deletedAt != null
                        }
                    )
                }
                verify(exactly = 0) { commentRepository.delete(any()) }
                comment.isDeleted shouldBe true
                comment.deletedAt.shouldNotBeNull()
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

            it("작성자가 아니면 NotFoundException 으로 응답한다") {
                val comment = basicComment(pageId = PageId(10L), authorId = UserId(200L))
                every { commentRepository.findBy(comment.id) } returns comment

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(currentUserId = UserId(100L)))
                }
                verify(exactly = 0) { commentRepository.save(any()) }
            }

            it("이미 삭제된 댓글이면 NotFoundException 으로 응답한다") {
                val comment = basicComment(pageId = PageId(10L)).also { it.delete() }
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
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "10",
            commentId: String = "1",
            currentUserId: UserId = UserId(100L)
        ): Request =
            Request(
                pageId = pageId,
                commentId = commentId,
                currentUserId = currentUserId
            )
    }
}
