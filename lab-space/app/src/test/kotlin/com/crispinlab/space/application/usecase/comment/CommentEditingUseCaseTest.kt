package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Request
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.usecase.mention.MentionDispatcher
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentContent
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Fixtures.basicComment
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import com.fasterxml.jackson.databind.ObjectMapper
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
        val pageRepository = mockk<PageRepository>()
        val spaceRepository = mockk<SpaceRepository>()
        val userHandleQuery = mockk<UserHandleQuery>()
        val mentionDispatcher = mockk<MentionDispatcher>(relaxed = true)
        val useCase =
            CommentEditingUseCase(
                commentRepository = commentRepository,
                pageRepository = pageRepository,
                spaceRepository = spaceRepository,
                userHandleQuery = userHandleQuery,
                mentionDispatcher = mentionDispatcher,
                transactionProvider = DummyTransactionProvider(),
                objectMapper = ObjectMapper()
            )

        beforeEach {
            clearMocks(
                commentRepository,
                pageRepository,
                spaceRepository,
                userHandleQuery,
                mentionDispatcher
            )
            every { pageRepository.findBy(any()) } returns basicPage()
            every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.PUBLIC
            every { commentRepository.save(any()) } answers { firstArg() }
            every { userHandleQuery.handlesOf(any()) } returns
                mapOf(
                    UserId(100L) to Handle("alice"),
                    UserId(200L) to Handle("bob")
                )
        }

        describe("댓글 수정") {
            it("content 를 갱신하고 Result 를 반환한다") {
                val comment = basicComment(pageId = PageId(10L), content = CommentContent("이전"))
                every { commentRepository.findBy(comment.id) } returns comment
                val saved = slot<Comment>()
                every { commentRepository.save(capture(saved)) } answers { saved.captured }

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            commentId = comment.id.value.toString(),
                            content = "수정됨"
                        )
                    )

                result.content shouldBe "수정됨"
                result.authorHandle shouldBe "alice"
                saved.captured.content.raw shouldBe "수정됨"
                verify(exactly = 1) { userHandleQuery.handlesOf(setOf(UserId(100L))) }
            }

            it("handle 조회가 비면 authorHandle 은 빈 문자열로 응답한다") {
                val comment = basicComment(pageId = PageId(10L))
                every { commentRepository.findBy(comment.id) } returns comment
                every { userHandleQuery.handlesOf(any()) } returns emptyMap()

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            commentId = comment.id.value.toString()
                        )
                    )

                result.authorHandle shouldBe ""
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
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { commentRepository.save(any()) }
            }

            it("ADMIN 은 작성자가 아니어도 수정 가능하다") {
                val comment =
                    basicComment(
                        pageId = PageId(10L),
                        authorId = UserId(200L),
                        content = CommentContent("이전")
                    )
                every { commentRepository.findBy(comment.id) } returns comment
                val saved = slot<Comment>()
                every { commentRepository.save(capture(saved)) } answers { saved.captured }

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            commentId = comment.id.value.toString(),
                            content = "수정됨",
                            userId = UserId(100L),
                            isAdmin = true
                        )
                    )

                result.content shouldBe "수정됨"
                result.authorHandle shouldBe "bob"
                saved.captured.content.raw shouldBe "수정됨"
                verify(exactly = 1) { userHandleQuery.handlesOf(setOf(UserId(200L))) }
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
                verify(exactly = 0) { commentRepository.save(any()) }
            }

            it("content 가 변경되지 않으면 save 와 mention dispatch 를 모두 skip 한다") {
                val comment =
                    basicComment(pageId = PageId(10L), content = CommentContent("동일 내용"))
                every { commentRepository.findBy(comment.id) } returns comment

                useCase.perform(
                    basicRequest(
                        pageId = "10",
                        commentId = comment.id.value.toString(),
                        content = "동일 내용"
                    )
                )

                verify(exactly = 0) { commentRepository.save(any()) }
                verify(exactly = 0) {
                    mentionDispatcher.dispatch(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                    )
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "10",
            commentId: String = "1",
            content: String = "수정된 댓글",
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                pageId = pageId,
                commentId = commentId,
                content = content,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}
