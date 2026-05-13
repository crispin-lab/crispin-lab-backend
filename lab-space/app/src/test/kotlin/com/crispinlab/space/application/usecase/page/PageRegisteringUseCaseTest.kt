package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.space.application.port.incoming.page.PageRegistering.Request
import com.crispinlab.space.application.port.outgoing.page.PageLinkRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageRevisionRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageLink
import com.crispinlab.space.domain.page.PageRevision
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class PageRegisteringUseCaseTest :
    DescribeSpec({
        val pageRepository = mockk<PageRepository>()
        val pageRevisionRepository = mockk<PageRevisionRepository>()
        val pageLinkRepository = mockk<PageLinkRepository>()
        val spaceRepository = mockk<SpaceRepository>()
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            PageRegisteringUseCase(
                pageRepository = pageRepository,
                pageRevisionRepository = pageRevisionRepository,
                pageLinkRepository = pageLinkRepository,
                spaceRepository = spaceRepository,
                idGenerator = idGenerator,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(
                pageRepository,
                pageRevisionRepository,
                pageLinkRepository,
                spaceRepository,
                idGenerator
            )
            every { spaceRepository.findBy(any()) } returns basicSpace()
            every { pageRepository.save(any()) } answers { firstArg() }
            every { pageRevisionRepository.save(any()) } answers { firstArg() }
            every { pageLinkRepository.saveAll(any()) } answers { firstArg() }
        }

        describe("페이지 생성") {
            it("페이지·초기 리비전·위키링크를 함께 저장한다") {
                every { idGenerator.next() } returnsMany listOf(1L, 2L, 3L, 4L, 5L)
                val savedPage = slot<Page>()
                val savedRevision = slot<PageRevision>()
                val savedLinks = slot<List<PageLink>>()
                every { pageRepository.save(capture(savedPage)) } answers { savedPage.captured }
                every { pageRevisionRepository.save(capture(savedRevision)) } answers
                    { savedRevision.captured }
                every { pageLinkRepository.saveAll(capture(savedLinks)) } answers
                    { savedLinks.captured }

                val result = useCase.perform(basicRequest(content = "본문 [[foo]] 와 [[bar|라벨]]"))

                result.pageId shouldBe "1"
                savedPage.captured.title shouldBe "테스트"
                savedPage.captured.currentVersion shouldBe 1
                savedRevision.captured.version shouldBe 1
                savedRevision.captured.pageId shouldBe savedPage.captured.id
                savedLinks.captured shouldHaveSize 2
                savedLinks.captured.map { it.target } shouldContainExactly listOf("foo", "bar")
            }

            it("본문에 위키링크가 없으면 saveAll 이 빈 리스트로 호출된다") {
                every { idGenerator.next() } returnsMany listOf(10L, 20L)
                val savedLinks = slot<List<PageLink>>()
                every { pageLinkRepository.saveAll(capture(savedLinks)) } answers
                    { savedLinks.captured }

                useCase.perform(basicRequest(content = "단순한 본문"))

                savedLinks.captured.shouldBeEmpty()
            }

            it("스페이스가 없으면 NotFoundException") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("부모 페이지가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(parentPageId = "99"))
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("부모 페이지가 다른 스페이스에 속하면 NotFoundException (존재/소속 응답 통합)") {
                every { pageRepository.findBy(any()) } returns basicPage(spaceId = SpaceId(999L))

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(parentPageId = "99"))
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "10",
            parentPageId: String? = null,
            title: String = "테스트",
            content: String = "본문",
            visibility: String = "DRAFT",
            currentUserId: UserId = UserId(100L)
        ): Request =
            Request(
                spaceId = spaceId,
                parentPageId = parentPageId,
                title = title,
                content = content,
                visibility = visibility,
                currentUserId = currentUserId
            )
    }
}
