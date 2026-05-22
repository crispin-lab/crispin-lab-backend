package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.space.application.port.incoming.page.PageEditing.Request
import com.crispinlab.space.application.port.outgoing.page.PageLinkRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageRevisionRepository
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageLink
import com.crispinlab.space.domain.page.PageRevision
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId
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

class PageEditingUseCaseTest :
    DescribeSpec({
        val pageRepository = mockk<PageRepository>()
        val pageRevisionRepository = mockk<PageRevisionRepository>()
        val pageLinkRepository = mockk<PageLinkRepository>()
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            PageEditingUseCase(
                pageRepository = pageRepository,
                pageRevisionRepository = pageRevisionRepository,
                pageLinkRepository = pageLinkRepository,
                idGenerator = idGenerator,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(pageRepository, pageRevisionRepository, pageLinkRepository, idGenerator)
            every { pageRepository.save(any()) } answers { firstArg() }
            every { pageRevisionRepository.save(any()) } answers { firstArg() }
            every { pageLinkRepository.saveAll(any()) } answers { firstArg() }
        }

        describe("페이지 수정") {
            it("제목·본문을 갱신하고 새 리비전·링크를 저장한다") {
                val page = basicPage(title = "이전 제목", currentVersion = 1)
                every { pageRepository.findBy(page.id) } returns page
                every { idGenerator.next() } returnsMany listOf(101L, 201L, 202L)
                val savedPage = slot<Page>()
                val savedRevision = slot<PageRevision>()
                val savedLinks = slot<List<PageLink>>()
                every { pageRepository.save(capture(savedPage)) } answers { savedPage.captured }
                every { pageRevisionRepository.save(capture(savedRevision)) } answers
                    { savedRevision.captured }
                every { pageLinkRepository.saveAll(capture(savedLinks)) } answers
                    { savedLinks.captured }

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = page.id.value.toString(),
                            title = "새 제목",
                            content = "본문 [[wiki1]] [[wiki2]]"
                        )
                    )

                result.title shouldBe "새 제목"
                result.version shouldBe 2
                savedPage.captured.currentVersion shouldBe 2
                savedRevision.captured.version shouldBe 2
                savedLinks.captured shouldHaveSize 2
                savedLinks.captured.map { it.target } shouldContainExactly listOf("wiki1", "wiki2")
            }

            it("위키링크 없는 본문은 saveAll 이 빈 리스트로 호출된다") {
                val page = basicPage()
                every { pageRepository.findBy(page.id) } returns page
                every { idGenerator.next() } returnsMany listOf(101L)
                val savedLinks = slot<List<PageLink>>()
                every { pageLinkRepository.saveAll(capture(savedLinks)) } answers
                    { savedLinks.captured }

                useCase.perform(basicRequest(pageId = page.id.value.toString(), content = "단순 본문"))

                savedLinks.captured.shouldBeEmpty()
            }

            it("페이지가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("작성자가 아니면 NotFoundException (존재/권한 응답 통합)") {
                val page = basicPage(authorId = UserId(200L))
                every { pageRepository.findBy(page.id) } returns page

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(currentUserId = UserId(100L)))
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "1",
            title: String = "새 제목",
            content: String = "새 본문",
            currentUserId: UserId = UserId(100L),
            currentUserRole: SystemRole = SystemRole.USER
        ): Request =
            Request(
                pageId = pageId,
                title = title,
                content = content,
                currentUserId = currentUserId,
                currentUserRole = currentUserRole
            )
    }
}
