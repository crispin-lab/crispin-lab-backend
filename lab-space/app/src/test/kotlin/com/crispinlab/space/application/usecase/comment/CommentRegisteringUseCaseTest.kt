package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Request
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.mention.MentionDispatcher
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
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

class CommentRegisteringUseCaseTest :
    DescribeSpec({
        val commentRepository = mockk<CommentRepository>()
        val pageRepository = mockk<PageRepository>()
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val userHandleQuery = mockk<UserHandleQuery>()
        val mentionDispatcher = mockk<MentionDispatcher>(relaxed = true)
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            CommentRegisteringUseCase(
                commentRepository = commentRepository,
                pageRepository = pageRepository,
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                userHandleQuery = userHandleQuery,
                mentionDispatcher = mentionDispatcher,
                idGenerator = idGenerator,
                transactionProvider = DummyTransactionProvider(),
                objectMapper = ObjectMapper()
            )

        beforeEach {
            clearMocks(
                commentRepository,
                pageRepository,
                spaceRepository,
                spaceMemberRepository,
                userHandleQuery,
                mentionDispatcher,
                idGenerator
            )
            every { pageRepository.findBy(any()) } returns basicPage()
            every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.PUBLIC
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
            every { commentRepository.save(any()) } answers { firstArg() }
            every { userHandleQuery.handlesOf(any()) } returns
                mapOf(UserId(100L) to Handle("test_user"))
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
                            content = "첫 댓글"
                        )
                    )

                result.commentId shouldBe CommentId(42L)
                result.authorHandle shouldBe "test_user"
                saved.captured.content.raw shouldBe "첫 댓글"
                saved.captured.pageId.value shouldBe 10L
                saved.captured.authorId.value shouldBe 100L
                verify(exactly = 1) { userHandleQuery.handlesOf(setOf(UserId(100L))) }
            }

            it("handle 조회가 비면 authorHandle 은 빈 문자열로 응답한다") {
                every { idGenerator.next() } returns 42L
                every { userHandleQuery.handlesOf(any()) } returns emptyMap()

                val result = useCase.perform(basicRequest())

                result.authorHandle shouldBe ""
            }

            it("Page 가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { commentRepository.save(any()) }
            }

            it("다른 사용자의 DRAFT 페이지에는 댓글을 달 수 없다") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(authorId = UserId(999L), visibility = Visibility.DRAFT)

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { commentRepository.save(any()) }
            }

            it("Space 비멤버 reader 도 PUBLIC 페이지에 댓글을 등록할 수 있다") {
                every { idGenerator.next() } returns 42L
                every { pageRepository.findBy(any()) } returns
                    basicPage(authorId = UserId(999L), visibility = Visibility.PUBLIC)

                useCase.perform(basicRequest())

                verify(exactly = 1) { commentRepository.save(any()) }
            }

            it("Space 멤버는 role 과 무관하게 MEMBER 페이지에 댓글을 등록할 수 있다") {
                every { idGenerator.next() } returns 42L
                every { pageRepository.findBy(any()) } returns
                    basicPage(
                        spaceId = SpaceId(10L),
                        authorId = UserId(999L),
                        visibility = Visibility.MEMBER
                    )
                every {
                    spaceMemberRepository.findSpaceIdsByUserId(UserId(100L))
                } returns setOf(SpaceId(10L))

                useCase.perform(basicRequest())

                verify(exactly = 1) { commentRepository.save(any()) }
            }

            it("INTERNAL space 의 author 본인은 비멤버 상태에서도 댓글을 등록할 수 있다") {
                every { idGenerator.next() } returns 42L
                every { pageRepository.findBy(any()) } returns
                    basicPage(
                        spaceId = SpaceId(10L),
                        authorId = UserId(100L),
                        visibility = Visibility.PUBLIC
                    )
                every { spaceRepository.findVisibility(SpaceId(10L)) } returns
                    SpaceVisibility.INTERNAL

                useCase.perform(basicRequest())

                verify(exactly = 1) { commentRepository.save(any()) }
            }

            it("ADMIN 은 다른 사용자의 DRAFT 페이지에도, 비멤버 상태에서도 댓글을 달 수 있다") {
                every { idGenerator.next() } returns 42L
                every { pageRepository.findBy(any()) } returns
                    basicPage(authorId = UserId(999L), visibility = Visibility.DRAFT)

                useCase.perform(basicRequest(isAdmin = true))

                verify(exactly = 1) { commentRepository.save(any()) }
            }

            it("content 가 비어 있으면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(content = "")
                }
                verify(exactly = 0) { commentRepository.save(any()) }
            }

            it("pageId 형식이 올바르지 않으면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(pageId = "not-a-number")
                }
            }

            it("cascade — INTERNAL space 의 PUBLIC 페이지에 비작성자가 댓글 등록 시 NotFoundException") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(authorId = UserId(999L), visibility = Visibility.PUBLIC)
                every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.INTERNAL

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { commentRepository.save(any()) }
            }

            it("cascade — dangling space 인 page 에 댓글 등록 시 NotFoundException") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(visibility = Visibility.PUBLIC)
                every { spaceRepository.findVisibility(any()) } returns null

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
            content: String = "댓글 내용",
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                pageId = pageId,
                content = content,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}
