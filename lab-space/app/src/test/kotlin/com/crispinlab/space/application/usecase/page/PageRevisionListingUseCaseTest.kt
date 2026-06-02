package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageRevisionListing.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageRevisionRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.Fixtures.basicPageRevision
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class PageRevisionListingUseCaseTest :
    DescribeSpec({
        val pageRepository = mockk<PageRepository>()
        val pageRevisionRepository = mockk<PageRevisionRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            PageRevisionListingUseCase(
                pageRepository = pageRepository,
                pageRevisionRepository = pageRevisionRepository,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(pageRepository, pageRevisionRepository, spaceMemberRepository)
            every { pageRepository.findBy(any()) } returns basicPage()
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
        }

        describe("페이지 리비전 목록 조회") {
            it("Page 의 리비전을 Summary 로 매핑해 반환한다") {
                val revisions =
                    listOf(
                        basicPageRevision(id = PageRevisionId(11L), version = 3, title = "세 번째"),
                        basicPageRevision(id = PageRevisionId(12L), version = 2, title = "두 번째"),
                        basicPageRevision(id = PageRevisionId(13L), version = 1, title = "초안")
                    )
                val capturedPageId = slot<PageId>()
                val capturedPageRequest = slot<PageRequest>()
                every {
                    pageRevisionRepository.findByPageId(
                        capture(capturedPageId),
                        capture(capturedPageRequest)
                    )
                } returns
                    PageResult(
                        items = revisions,
                        page = 0,
                        size = 10,
                        totalElements = 3L
                    )

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "1",
                            page = 0,
                            size = 10
                        )
                    )

                result.items.map { it.version } shouldBe listOf(3, 2, 1)
                result.items.map { it.title } shouldBe listOf("세 번째", "두 번째", "초안")
                result.totalElements shouldBe 3L
                capturedPageId.captured.value shouldBe 1L
                capturedPageRequest.captured.page shouldBe 0
                capturedPageRequest.captured.size shouldBe 10
            }

            it("Page 가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) {
                    pageRevisionRepository.findByPageId(any(), any())
                }
            }

            it("비로그인 상태에서 INTERNAL 페이지는 NotFoundException") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(visibility = Visibility.INTERNAL)

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(viewer = Viewer.Anonymous))
                }
                verify(exactly = 0) {
                    pageRevisionRepository.findByPageId(any(), any())
                }
            }

            it("비로그인 상태에서 PUBLIC 페이지는 정상 조회된다") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(visibility = Visibility.PUBLIC)
                every {
                    pageRevisionRepository.findByPageId(any(), any())
                } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = DEFAULT_SIZE,
                        totalElements = 0L
                    )

                useCase.perform(basicRequest(viewer = Viewer.Anonymous))

                verify(exactly = 1) {
                    pageRevisionRepository.findByPageId(any(), any())
                }
            }

            it("USER 가 다른 사용자의 DRAFT 페이지를 보면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(authorId = UserId(999L), visibility = Visibility.DRAFT)

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
            }

            it("ADMIN 은 다른 사용자의 DRAFT 페이지 리비전도 조회 가능하다") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(authorId = UserId(999L), visibility = Visibility.DRAFT)
                every {
                    pageRevisionRepository.findByPageId(any(), any())
                } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = DEFAULT_SIZE,
                        totalElements = 0L
                    )

                useCase.perform(
                    basicRequest(
                        viewer = Viewer.Member(userId = UserId(100L), isAdmin = true)
                    )
                )

                verify(exactly = 1) {
                    pageRevisionRepository.findByPageId(any(), any())
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "1",
            page: Int = 0,
            size: Int = DEFAULT_SIZE,
            viewer: Viewer = Viewer.Member(userId = UserId(100L), isAdmin = false)
        ): Request =
            Request(
                pageId = pageId,
                page = page,
                size = size,
                viewer = viewer
            )
    }
}
