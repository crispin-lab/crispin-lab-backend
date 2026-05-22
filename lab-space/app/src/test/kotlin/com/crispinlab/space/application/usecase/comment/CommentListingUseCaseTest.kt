package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Request
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicComment
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class CommentListingUseCaseTest :
    DescribeSpec({
        val commentRepository = mockk<CommentRepository>()
        val pageRepository = mockk<PageRepository>()
        val useCase =
            CommentListingUseCase(
                commentRepository = commentRepository,
                pageRepository = pageRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(commentRepository, pageRepository)
            every { pageRepository.findBy(any()) } returns basicPage()
        }

        describe("댓글 목록 조회") {
            it("Page 의 댓글을 Summary 로 매핑해 반환한다") {
                val comments: List<Comment> =
                    listOf(
                        basicComment(id = CommentId(1L), body = "첫 댓글"),
                        basicComment(id = CommentId(2L), body = "두 번째")
                    )
                val capturedPageId = slot<PageId>()
                val capturedPageRequest = slot<PageRequest>()
                every {
                    commentRepository.findByPageId(
                        capture(capturedPageId),
                        capture(capturedPageRequest)
                    )
                } returns
                    PageResult(
                        items = comments,
                        page = 1,
                        size = 5,
                        totalElements = 7L
                    )

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            page = 1,
                            size = 5
                        )
                    )

                result.items.map { it.commentId } shouldBe listOf(CommentId(1L), CommentId(2L))
                result.items.map { it.body } shouldBe listOf("첫 댓글", "두 번째")
                result.totalElements shouldBe 7L
                result.page shouldBe 1
                result.size shouldBe 5
                capturedPageId.captured.value shouldBe 10L
                capturedPageRequest.captured.page shouldBe 1
                capturedPageRequest.captured.size shouldBe 5
            }

            it("결과가 비어 있어도 빈 페이지를 반환한다") {
                every { commentRepository.findByPageId(any(), any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                val result = useCase.perform(basicRequest())

                result.items shouldBe emptyList()
                result.totalElements shouldBe 0L
            }

            it("Page 가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { commentRepository.findByPageId(any(), any()) }
            }

            it("page 가 음수면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(page = -1)
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "10",
            page: Int = 0,
            size: Int = DEFAULT_SIZE,
            currentUserId: UserId = UserId(100L)
        ): Request =
            Request(
                pageId = pageId,
                page = page,
                size = size,
                currentUserId = currentUserId
            )
    }
}
