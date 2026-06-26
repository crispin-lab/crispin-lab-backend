package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Request
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Fixtures.basicComment
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CommentGettingUseCaseTest :
    DescribeSpec({
        val commentRepository = mockk<CommentRepository>()
        val pageRepository = mockk<PageRepository>()
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val userHandleQuery = mockk<UserHandleQuery>()
        val useCase =
            CommentGettingUseCase(
                commentRepository = commentRepository,
                pageRepository = pageRepository,
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                userHandleQuery = userHandleQuery,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(
                commentRepository,
                pageRepository,
                spaceRepository,
                spaceMemberRepository,
                userHandleQuery
            )
            every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.PUBLIC
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
            every { userHandleQuery.handlesOf(any()) } returns
                mapOf(UserId(100L) to Handle("test_user"))
        }

        describe("댓글 단건 조회") {
            it("PUBLIC 페이지의 댓글을 정상 조회한다") {
                val page = basicPage(id = PageId(10L), visibility = Visibility.PUBLIC)
                val comment =
                    basicComment(
                        id = CommentId(7L),
                        pageId = page.id,
                        body = "안녕하세요"
                    )
                every { pageRepository.findBy(page.id) } returns page
                every { commentRepository.findBy(comment.id) } returns comment

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            commentId = "7"
                        )
                    )

                result.commentId shouldBe CommentId(7L)
                result.body shouldBe "안녕하세요"
                result.authorId shouldBe UserId(100L)
                result.authorHandle shouldBe "test_user"
                verify(exactly = 1) { userHandleQuery.handlesOf(setOf(UserId(100L))) }
            }

            it("author 가 삭제된 사용자라 handle 조회가 비면 authorHandle 은 빈 문자열로 응답한다") {
                val page = basicPage(id = PageId(10L), visibility = Visibility.PUBLIC)
                val comment = basicComment(pageId = page.id)
                every { pageRepository.findBy(page.id) } returns page
                every { commentRepository.findBy(comment.id) } returns comment
                every { userHandleQuery.handlesOf(any()) } returns emptyMap()

                val result = useCase.perform(basicRequest(pageId = "10"))

                result.authorHandle shouldBe ""
            }

            it("페이지가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { commentRepository.findBy(any()) }
            }

            it("다른 사용자의 DRAFT 페이지는 PAGE_NOT_FOUND") {
                val page =
                    basicPage(
                        id = PageId(10L),
                        authorId = UserId(999L),
                        visibility = Visibility.DRAFT
                    )
                every { pageRepository.findBy(page.id) } returns page

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { commentRepository.findBy(any()) }
            }

            it("본인의 DRAFT 페이지는 조회할 수 있다") {
                val page =
                    basicPage(
                        id = PageId(10L),
                        authorId = UserId(100L),
                        visibility = Visibility.DRAFT
                    )
                val comment = basicComment(pageId = page.id)
                every { pageRepository.findBy(page.id) } returns page
                every { commentRepository.findBy(comment.id) } returns comment

                val result = useCase.perform(basicRequest(userId = UserId(100L)))

                result.commentId shouldBe comment.id
            }

            it("ADMIN 은 다른 사용자의 DRAFT 페이지의 댓글도 조회할 수 있다") {
                val page =
                    basicPage(
                        id = PageId(10L),
                        authorId = UserId(999L),
                        visibility = Visibility.DRAFT
                    )
                val comment = basicComment(pageId = page.id)
                every { pageRepository.findBy(page.id) } returns page
                every { commentRepository.findBy(comment.id) } returns comment

                val result = useCase.perform(basicRequest(isAdmin = true))

                result.commentId shouldBe comment.id
            }

            it("댓글이 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(visibility = Visibility.PUBLIC)
                every { commentRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
            }

            it("URL 의 pageId 와 댓글의 pageId 가 다르면 NotFoundException") {
                val page = basicPage(id = PageId(10L), visibility = Visibility.PUBLIC)
                val comment = basicComment(pageId = PageId(999L))
                every { pageRepository.findBy(page.id) } returns page
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

            it("cascade — INTERNAL space 의 PUBLIC 페이지 댓글은 비작성자에게 NotFoundException") {
                val page =
                    basicPage(
                        id = PageId(10L),
                        authorId = UserId(999L),
                        visibility = Visibility.PUBLIC
                    )
                every { pageRepository.findBy(page.id) } returns page
                every { spaceRepository.findVisibility(page.spaceId) } returns
                    SpaceVisibility.INTERNAL

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { commentRepository.findBy(any()) }
            }

            it("cascade — dangling space (findVisibility=null) 인 page 의 댓글은 NotFoundException") {
                val page = basicPage(id = PageId(10L), visibility = Visibility.PUBLIC)
                every { pageRepository.findBy(page.id) } returns page
                every { spaceRepository.findVisibility(page.spaceId) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { commentRepository.findBy(any()) }
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
