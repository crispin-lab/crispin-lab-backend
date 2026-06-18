package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageRevisionGetting.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageRevisionRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageRevisionErrorCode
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.Fixtures.basicPageRevision
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PageRevisionGettingUseCaseTest :
    DescribeSpec({
        val pageRepository = mockk<PageRepository>()
        val pageRevisionRepository = mockk<PageRevisionRepository>()
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            PageRevisionGettingUseCase(
                pageRepository = pageRepository,
                pageRevisionRepository = pageRevisionRepository,
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(
                pageRepository,
                pageRevisionRepository,
                spaceRepository,
                spaceMemberRepository
            )
            every { pageRepository.findBy(any()) } returns basicPage()
            every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.PUBLIC
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
        }

        describe("페이지 리비전 단건 조회") {
            it("정상적으로 조회한다") {
                val revision =
                    basicPageRevision(
                        id = PageRevisionId(11L),
                        pageId = PageId(1L),
                        version = 2,
                        title = "두 번째"
                    )
                every {
                    pageRevisionRepository.findBy(PageId(1L), version = 2)
                } returns revision

                val result = useCase.perform(basicRequest(pageId = "1", version = 2))

                result.revisionId shouldBe PageRevisionId(11L)
                result.version shouldBe 2
                result.title shouldBe "두 번째"
            }

            it("Page 가 없으면 PAGE_NOT_FOUND 로 응답한다") {
                every { pageRepository.findBy(any()) } returns null

                val exception =
                    shouldThrow<NotFoundException> {
                        useCase.perform(basicRequest())
                    }
                exception.errorCode shouldBe PageErrorCode.PAGE_NOT_FOUND
                verify(exactly = 0) {
                    pageRevisionRepository.findBy(any<PageId>(), any<Int>())
                }
            }

            it("Page 권한이 없으면 PAGE_NOT_FOUND 로 응답한다") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(authorId = UserId(999L), visibility = Visibility.DRAFT)

                val exception =
                    shouldThrow<NotFoundException> {
                        useCase.perform(basicRequest())
                    }
                exception.errorCode shouldBe PageErrorCode.PAGE_NOT_FOUND
                verify(exactly = 0) {
                    pageRevisionRepository.findBy(any<PageId>(), any<Int>())
                }
            }

            it("Page 는 보여도 해당 version 의 리비전이 없으면 PAGE_REVISION_NOT_FOUND") {
                every {
                    pageRevisionRepository.findBy(any<PageId>(), any<Int>())
                } returns null

                val exception =
                    shouldThrow<NotFoundException> {
                        useCase.perform(basicRequest(version = 99))
                    }
                exception.errorCode shouldBe PageRevisionErrorCode.PAGE_REVISION_NOT_FOUND
            }

            it("비로그인 상태에서 PUBLIC 페이지의 리비전은 조회 가능하다") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(visibility = Visibility.PUBLIC)
                every {
                    pageRevisionRepository.findBy(any<PageId>(), any<Int>())
                } returns basicPageRevision()

                useCase.perform(basicRequest(viewer = Viewer.Anonymous))

                verify(exactly = 1) {
                    pageRevisionRepository.findBy(any<PageId>(), any<Int>())
                }
            }

            it("version 이 1 미만이면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(version = 0)
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "1",
            version: Int = 1,
            viewer: Viewer = Viewer.Member(userId = UserId(100L), isAdmin = false)
        ): Request =
            Request(
                pageId = pageId,
                version = version,
                viewer = viewer
            )
    }
}
