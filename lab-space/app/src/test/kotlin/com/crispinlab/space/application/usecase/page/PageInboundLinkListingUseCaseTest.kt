package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing.Request
import com.crispinlab.space.application.port.outgoing.page.PageInboundLinkPort
import com.crispinlab.space.application.port.outgoing.page.PageInboundLinkPort.InboundLinkSummary
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
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
import io.mockk.slot
import io.mockk.verify

class PageInboundLinkListingUseCaseTest :
    DescribeSpec({
        val pageRepository = mockk<PageRepository>()
        val pageInboundLinkPort = mockk<PageInboundLinkPort>()
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val userHandleQuery = mockk<UserHandleQuery>()
        val useCase =
            PageInboundLinkListingUseCase(
                pageRepository = pageRepository,
                pageInboundLinkPort = pageInboundLinkPort,
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                userHandleQuery = userHandleQuery,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(
                pageRepository,
                pageInboundLinkPort,
                spaceRepository,
                spaceMemberRepository,
                userHandleQuery
            )
            every { pageRepository.findBy(any()) } returns basicPage(visibility = Visibility.PUBLIC)
            every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.PUBLIC
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
        }

        describe("페이지 인바운드 링크 목록 조회") {
            it("어댑터가 반환한 PageSummary 를 authorHandle 을 채워 Summary 로 매핑한다") {
                val capturedTargetPageId = slot<PageId>()
                val capturedScope = slot<VisibilityScope>()
                val capturedPageRequest = slot<PageRequest>()
                every {
                    pageInboundLinkPort.findInboundLinksOf(
                        capture(capturedTargetPageId),
                        capture(capturedScope),
                        capture(capturedPageRequest)
                    )
                } returns
                    PageResult(
                        items =
                            listOf(
                                basicSummary(
                                    id = PageId(11L),
                                    authorId = UserId(100L),
                                    title = "이전 회고"
                                ),
                                basicSummary(
                                    id = PageId(12L),
                                    authorId = UserId(200L),
                                    title = "분기 회고"
                                )
                            ),
                        page = 0,
                        size = DEFAULT_SIZE,
                        totalElements = 2L
                    )
                every { userHandleQuery.handlesOf(any()) } returns
                    mapOf(
                        UserId(100L) to Handle("alice"),
                        UserId(200L) to Handle("bob")
                    )

                val result = useCase.perform(basicRequest(pageId = "42"))

                result.items.map { it.pageId } shouldBe listOf(PageId(11L), PageId(12L))
                result.items.map { it.authorHandle } shouldBe listOf("alice", "bob")
                result.totalElements shouldBe 2L
                capturedTargetPageId.captured.value shouldBe 42L
                capturedPageRequest.captured.page shouldBe 0
                capturedPageRequest.captured.size shouldBe DEFAULT_SIZE
            }

            it("target 페이지가 없으면 NotFoundException — 어댑터를 호출하지 않는다") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) {
                    pageInboundLinkPort.findInboundLinksOf(any(), any(), any())
                }
            }

            it("비로그인 상태에서 INTERNAL target 은 NotFoundException — 어댑터를 호출하지 않는다") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(visibility = Visibility.INTERNAL)

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(viewer = Viewer.Anonymous))
                }
                verify(exactly = 0) {
                    pageInboundLinkPort.findInboundLinksOf(any(), any(), any())
                }
            }

            it("비로그인 상태에서 PUBLIC target 은 정상 조회되고 어댑터에 Anonymous scope 가 전달된다") {
                val capturedScope = slot<VisibilityScope>()
                every {
                    pageInboundLinkPort.findInboundLinksOf(any(), capture(capturedScope), any())
                } returns PageResult.empty(PageRequest.firstPage())

                useCase.perform(basicRequest(viewer = Viewer.Anonymous))

                capturedScope.captured shouldBe VisibilityScope.Anonymous
            }

            it("ADMIN 호출에는 Privileged scope 가 전달된다") {
                val capturedScope = slot<VisibilityScope>()
                every {
                    pageInboundLinkPort.findInboundLinksOf(any(), capture(capturedScope), any())
                } returns PageResult.empty(PageRequest.firstPage())

                useCase.perform(
                    basicRequest(viewer = Viewer.Member(userId = UserId(100L), isAdmin = true))
                )

                capturedScope.captured shouldBe VisibilityScope.Privileged
            }

            it("UserHandleQuery 에서 누락된 author 는 빈 문자열로 채운다") {
                every {
                    pageInboundLinkPort.findInboundLinksOf(any(), any(), any())
                } returns
                    PageResult(
                        items =
                            listOf(
                                basicSummary(id = PageId(11L), authorId = UserId(100L)),
                                basicSummary(id = PageId(12L), authorId = UserId(200L))
                            ),
                        page = 0,
                        size = DEFAULT_SIZE,
                        totalElements = 2L
                    )
                every { userHandleQuery.handlesOf(any()) } returns
                    mapOf(UserId(100L) to Handle("alice"))

                val result = useCase.perform(basicRequest())

                result.items.map { it.authorHandle } shouldBe listOf("alice", "")
            }

            it("어댑터가 빈 결과를 돌려주면 UserHandleQuery 를 호출하지 않는다") {
                every {
                    pageInboundLinkPort.findInboundLinksOf(any(), any(), any())
                } returns PageResult.empty(PageRequest.firstPage())

                val result = useCase.perform(basicRequest())

                result.items shouldBe emptyList()
                verify(exactly = 0) { userHandleQuery.handlesOf(any()) }
            }

            it("cascade — INTERNAL space 의 PUBLIC target 은 anonymous 에게 NotFoundException") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(visibility = Visibility.PUBLIC)
                every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.INTERNAL

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(viewer = Viewer.Anonymous))
                }
                verify(exactly = 0) {
                    pageInboundLinkPort.findInboundLinksOf(any(), any(), any())
                }
            }

            it("cascade — dangling space 인 target 은 NotFoundException") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(visibility = Visibility.PUBLIC)
                every { spaceRepository.findVisibility(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) {
                    pageInboundLinkPort.findInboundLinksOf(any(), any(), any())
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

        fun basicSummary(
            id: PageId = PageId(11L),
            spaceId: SpaceId = SpaceId(10L),
            parentPageId: PageId? = null,
            authorId: UserId = UserId(100L),
            title: String = "이전 페이지",
            visibility: Visibility = Visibility.PUBLIC,
            displayOrder: Int = 0
        ): InboundLinkSummary =
            InboundLinkSummary(
                pageId = id,
                spaceId = spaceId,
                parentPageId = parentPageId,
                authorId = authorId,
                title = title,
                visibility = visibility,
                displayOrder = displayOrder,
                updatedAt = DUMMY_INSTANT
            )
    }
}
