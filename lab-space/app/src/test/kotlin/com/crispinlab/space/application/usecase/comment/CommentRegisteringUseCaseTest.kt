package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Request
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.testsupport.DummyTransactionProvider
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

class CommentRegisteringUseCaseTest :
    DescribeSpec({
        val commentRepository = mockk<CommentRepository>()
        val pageRepository = mockk<PageRepository>()
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            CommentRegisteringUseCase(
                commentRepository = commentRepository,
                pageRepository = pageRepository,
                idGenerator = idGenerator,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(commentRepository, pageRepository, idGenerator)
            every { pageRepository.findBy(any()) } returns basicPage()
            every { commentRepository.save(any()) } answers { firstArg() }
        }

        describe("댓글 등록") {
            it("Page 가 존재하면 Comment 를 저장하고 commentId 를 반환한다") {
                every { idGenerator.next() } returns 42L
                val saved = slot<Comment>()
                every { commentRepository.save(capture(saved)) } answers { saved.captured }

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            body = "첫 댓글"
                        )
                    )

                result.commentId shouldBe CommentId(42L)
                saved.captured.body shouldBe "첫 댓글"
                saved.captured.pageId.value shouldBe 10L
                saved.captured.authorId.value shouldBe 100L
            }

            it("Page 가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { commentRepository.save(any()) }
            }

            it("body 가 비어 있으면 entity 생성에서 실패한다") {
                every { idGenerator.next() } returns 1L

                shouldThrow<IllegalArgumentException> {
                    useCase.perform(basicRequest(body = ""))
                }
                verify(exactly = 0) { commentRepository.save(any()) }
            }

            it("pageId 형식이 올바르지 않으면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(pageId = "not-a-number")
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "10",
            body: String = "댓글 내용",
            currentUserId: UserId = UserId(100L)
        ): Request =
            Request(
                pageId = pageId,
                body = body,
                currentUserId = currentUserId
            )
    }
}
