package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Request
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.Comment
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
import io.mockk.slot
import io.mockk.verify

class CommentListingUseCaseTest :
    DescribeSpec({
        val commentRepository = mockk<CommentRepository>()
        val pageRepository = mockk<PageRepository>()
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            CommentListingUseCase(
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
            every { pageRepository.findBy(any()) } returns basicPage()
            every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.PUBLIC
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
            every { spaceMemberRepository.findBySpaceIdAndUserId(any(), any()) } returns null
        }

        describe("댓글 목록 조회") {
            it("Page 의 댓글을 Summary 로 매핑해 반환한다") {
                val comments: List<Comment> =
                    listOf(
                        basicComment(
                            id = CommentId(1L),
                            authorId = UserId(100L),
                            content = CommentContent("첫 댓글")
                        ),
                        basicComment(
                            id = CommentId(2L),
                            authorId = UserId(101L),
                            content = CommentContent("두 번째")
                        )
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
                result.items.map { it.authorId } shouldBe listOf(UserId(100L), UserId(101L))
                result.items.map { it.content } shouldBe listOf("첫 댓글", "두 번째")
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

            it("다른 사용자의 DRAFT 페이지는 NotFoundException") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(authorId = UserId(999L), visibility = Visibility.DRAFT)

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { commentRepository.findByPageId(any(), any()) }
            }

            it("ADMIN 은 다른 사용자의 DRAFT 페이지의 댓글 목록도 조회할 수 있다") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(authorId = UserId(999L), visibility = Visibility.DRAFT)
                every { commentRepository.findByPageId(any(), any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                useCase.perform(basicRequest(isAdmin = true))

                verify(exactly = 1) { commentRepository.findByPageId(any(), any()) }
            }

            it("page 가 음수면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(page = -1)
                }
            }

            it("cascade — INTERNAL space 의 PUBLIC 페이지 댓글 목록은 비작성자에게 NotFoundException") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(authorId = UserId(999L), visibility = Visibility.PUBLIC)
                every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.INTERNAL

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { commentRepository.findByPageId(any(), any()) }
            }

            it("cascade — dangling space 인 page 의 댓글 목록은 NotFoundException") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(visibility = Visibility.PUBLIC)
                every { spaceRepository.findVisibility(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { commentRepository.findByPageId(any(), any()) }
            }
        }

        describe("각 Summary 의 canEdit — viewer 의 수정 권한 노출") {
            it("ADMIN 은 본인/타인 댓글 모두 canEdit=true") {
                every { commentRepository.findByPageId(any(), any()) } returns
                    PageResult(
                        items =
                            listOf(
                                basicComment(id = CommentId(1L), authorId = UserId(100L)),
                                basicComment(id = CommentId(2L), authorId = UserId(999L))
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                val result = useCase.perform(basicRequest(isAdmin = true))

                result.items.map { it.canEdit } shouldBe listOf(true, true)
            }

            it("일반 USER 는 본인 댓글만 canEdit=true") {
                every { commentRepository.findByPageId(any(), any()) } returns
                    PageResult(
                        items =
                            listOf(
                                basicComment(id = CommentId(1L), authorId = UserId(100L)),
                                basicComment(id = CommentId(2L), authorId = UserId(100L)),
                                basicComment(id = CommentId(3L), authorId = UserId(999L))
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 3L
                    )

                val result = useCase.perform(basicRequest(userId = UserId(100L)))

                result.items.map { it.canEdit } shouldBe listOf(true, true, false)
            }

            it("본인 댓글이 없으면 canEdit 전부 false") {
                every { commentRepository.findByPageId(any(), any()) } returns
                    PageResult(
                        items =
                            listOf(
                                basicComment(id = CommentId(1L), authorId = UserId(999L)),
                                basicComment(id = CommentId(2L), authorId = UserId(998L))
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                val result = useCase.perform(basicRequest(userId = UserId(100L)))

                result.items.map { it.canEdit } shouldBe listOf(false, false)
            }

            it("스페이스 비멤버 author 도 자기 댓글은 canEdit=true (스페이스 멤버십 무관)") {
                every { commentRepository.findByPageId(any(), any()) } returns
                    PageResult(
                        items = listOf(basicComment(authorId = UserId(100L))),
                        page = 0,
                        size = 20,
                        totalElements = 1L
                    )

                val result = useCase.perform(basicRequest(userId = UserId(100L)))

                result.items.single().canEdit shouldBe true
            }

            it("VIEWER role author 도 자기 댓글은 canEdit=true (스페이스 write role 무관)") {
                val page = basicPage()
                every { pageRepository.findBy(any()) } returns page
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(page.spaceId, UserId(100L))
                } returns basicSpaceMember(role = SpaceMemberRole.VIEWER)
                every { commentRepository.findByPageId(any(), any()) } returns
                    PageResult(
                        items = listOf(basicComment(authorId = UserId(100L))),
                        page = 0,
                        size = 20,
                        totalElements = 1L
                    )

                val result = useCase.perform(basicRequest(userId = UserId(100L)))

                result.items.single().canEdit shouldBe true
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "10",
            page: Int = 0,
            size: Int = DEFAULT_SIZE,
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                pageId = pageId,
                page = page,
                size = size,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}
