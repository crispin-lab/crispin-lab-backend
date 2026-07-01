package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Request
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentContent
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Fixtures.basicComment
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
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
        val useCase =
            CommentGettingUseCase(
                commentRepository = commentRepository,
                pageRepository = pageRepository,
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(
                commentRepository,
                pageRepository,
                spaceRepository,
                spaceMemberRepository
            )
            every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.PUBLIC
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
            every { spaceMemberRepository.findBySpaceIdAndUserId(any(), any()) } returns null
        }

        describe("댓글 단건 조회") {
            it("PUBLIC 페이지의 댓글을 정상 조회한다") {
                val page = basicPage(id = PageId(10L), visibility = Visibility.PUBLIC)
                val comment =
                    basicComment(
                        id = CommentId(7L),
                        pageId = page.id,
                        content = CommentContent("안녕하세요")
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
                result.content shouldBe "안녕하세요"
                result.authorId shouldBe UserId(100L)
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

        describe("응답의 canEdit — viewer 의 수정 권한 노출") {
            it("ADMIN 이면 canEdit=true") {
                val page = basicPage(id = PageId(10L), visibility = Visibility.PUBLIC)
                val comment =
                    basicComment(pageId = page.id, authorId = UserId(999L))
                every { pageRepository.findBy(page.id) } returns page
                every { commentRepository.findBy(comment.id) } returns comment

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            commentId = comment.id.value.toString(),
                            isAdmin = true
                        )
                    )

                result.canEdit shouldBe true
            }

            it("본인 댓글 + 스페이스 쓰기 권한 보유면 canEdit=true") {
                val page = basicPage(id = PageId(10L), visibility = Visibility.PUBLIC)
                val comment = basicComment(pageId = page.id, authorId = UserId(100L))
                every { pageRepository.findBy(page.id) } returns page
                every { commentRepository.findBy(comment.id) } returns comment
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(page.spaceId, UserId(100L))
                } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            commentId = comment.id.value.toString(),
                            userId = UserId(100L)
                        )
                    )

                result.canEdit shouldBe true
            }

            it("본인 댓글이지만 스페이스 쓰기 권한이 없으면 canEdit=false (VIEWER role)") {
                val page = basicPage(id = PageId(10L), visibility = Visibility.PUBLIC)
                val comment = basicComment(pageId = page.id, authorId = UserId(100L))
                every { pageRepository.findBy(page.id) } returns page
                every { commentRepository.findBy(comment.id) } returns comment
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(page.spaceId, UserId(100L))
                } returns basicSpaceMember(role = SpaceMemberRole.VIEWER)

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            commentId = comment.id.value.toString(),
                            userId = UserId(100L)
                        )
                    )

                result.canEdit shouldBe false
            }

            it("타인 댓글이면 canEdit=false (일반 USER)") {
                val page = basicPage(id = PageId(10L), visibility = Visibility.PUBLIC)
                val comment = basicComment(pageId = page.id, authorId = UserId(999L))
                every { pageRepository.findBy(page.id) } returns page
                every { commentRepository.findBy(comment.id) } returns comment
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(page.spaceId, UserId(100L))
                } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            commentId = comment.id.value.toString(),
                            userId = UserId(100L)
                        )
                    )

                result.canEdit shouldBe false
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
