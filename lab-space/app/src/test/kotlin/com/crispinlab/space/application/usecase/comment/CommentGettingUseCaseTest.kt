package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Request
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.domain.comment.CommentId
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

class CommentGettingUseCaseTest :
    DescribeSpec({
        val commentRepository = mockk<CommentRepository>()
        val useCase = CommentGettingUseCase(commentRepository, DummyTransactionProvider())

        beforeEach {
            clearMocks(commentRepository)
        }

        describe("댓글 단건 조회") {
            it("정상적으로 조회한다") {
                val comment =
                    basicComment(
                        id = CommentId(7L),
                        pageId = PageId(10L),
                        body = "안녕하세요"
                    )
                every { commentRepository.findBy(comment.id) } returns comment

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            commentId = "7"
                        )
                    )

                result.commentId shouldBe "7"
                result.body shouldBe "안녕하세요"
                result.deletedAt shouldBe null
            }

            it("삭제된 댓글도 그대로 반환한다 (소비자가 isDeleted 판단)") {
                val comment = basicComment(pageId = PageId(10L)).also { it.delete() }
                every { commentRepository.findBy(comment.id) } returns comment

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            commentId = comment.id.value.toString()
                        )
                    )

                result.deletedAt shouldBe comment.deletedAt
            }

            it("댓글이 없으면 NotFoundException") {
                every { commentRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
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
            }

            it("ID 형식이 올바르지 않으면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(commentId = "not-a-number")
                }
                shouldThrow<IllegalArgumentException> {
                    basicRequest(pageId = "not-a-number")
                }
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
