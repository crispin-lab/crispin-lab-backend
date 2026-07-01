package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageRegistering.Request
import com.crispinlab.space.application.port.outgoing.page.PageLinkRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageRevisionRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.mention.MentionDispatcher
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageLink
import com.crispinlab.space.domain.page.PageRevision
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
import com.crispinlab.space.testsupport.TipTapJsonFixtures.doc
import com.crispinlab.space.testsupport.TipTapJsonFixtures.pageLink
import com.crispinlab.space.testsupport.TipTapJsonFixtures.paragraph
import com.crispinlab.space.testsupport.TipTapJsonFixtures.text
import com.crispinlab.user.domain.user.UserId
import com.fasterxml.jackson.databind.ObjectMapper
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
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val mentionDispatcher = mockk<MentionDispatcher>(relaxed = true)
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            PageRegisteringUseCase(
                pageRepository = pageRepository,
                pageRevisionRepository = pageRevisionRepository,
                pageLinkRepository = pageLinkRepository,
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                mentionDispatcher = mentionDispatcher,
                idGenerator = idGenerator,
                transactionProvider = DummyTransactionProvider(),
                objectMapper = ObjectMapper()
            )

        beforeEach {
            clearMocks(
                pageRepository,
                pageRevisionRepository,
                pageLinkRepository,
                spaceRepository,
                spaceMemberRepository,
                mentionDispatcher,
                idGenerator
            )
            every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.PUBLIC
            every {
                spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
            } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)
            every { pageRepository.nextDisplayOrderIn(any(), any()) } returns 0
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

                val result =
                    useCase.perform(
                        basicRequest(
                            content =
                                doc(
                                    paragraph(
                                        text("본문 "),
                                        pageLink(
                                            pageId = 7L,
                                            displayText = "라벨"
                                        ),
                                        text(" 와 "),
                                        pageLink(
                                            pageId = 8L,
                                            displayText = "두번째"
                                        )
                                    )
                                )
                        )
                    )

                result.pageId shouldBe PageId(1L)
                savedPage.captured.title shouldBe "테스트"
                savedPage.captured.currentVersion shouldBe 1
                savedRevision.captured.version shouldBe 1
                savedRevision.captured.pageId shouldBe savedPage.captured.id
                savedLinks.captured shouldHaveSize 2
                savedLinks.captured.map { it.target } shouldContainExactly
                    listOf(PageId(7L), PageId(8L))
            }

            it("본문에 pageLink 가 없으면 saveAll 이 빈 리스트로 호출된다") {
                every { idGenerator.next() } returnsMany listOf(10L, 20L)
                val savedLinks = slot<List<PageLink>>()
                every { pageLinkRepository.saveAll(capture(savedLinks)) } answers
                    { savedLinks.captured }

                useCase.perform(basicRequest(content = doc(paragraph(text("단순한 본문")))))

                savedLinks.captured.shouldBeEmpty()
            }

            it("스페이스가 없으면 NotFoundException") {
                every { spaceRepository.findVisibility(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("멤버가 아니면 ForbiddenException 으로 차단된다") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns null

                shouldThrow<ForbiddenException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("VIEWER role 은 ForbiddenException 으로 차단된다") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns basicSpaceMember(role = SpaceMemberRole.VIEWER)

                shouldThrow<ForbiddenException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("ADMIN 은 멤버가 아니어도 페이지를 생성할 수 있다") {
                every { idGenerator.next() } returnsMany listOf(1L, 2L)

                useCase.perform(basicRequest(isAdmin = true))

                verify(exactly = 1) { pageRepository.save(any()) }
                verify(exactly = 0) {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                }
            }

            it("부모 페이지가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(parentPageId = "99"))
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("같은 (spaceId, parentPageId) scope 의 MAX+1 을 displayOrder 로 자동 할당한다") {
                every { idGenerator.next() } returnsMany listOf(1L, 2L, 3L)
                every {
                    pageRepository.nextDisplayOrderIn(SpaceId(10L), PageId(7L))
                } returns 5
                val savedPage = slot<Page>()
                every { pageRepository.save(capture(savedPage)) } answers
                    { savedPage.captured }
                every { pageRepository.findBy(PageId(7L)) } returns
                    basicPage(id = PageId(7L), spaceId = SpaceId(10L))

                useCase.perform(
                    basicRequest(spaceId = "10", parentPageId = "7")
                )

                savedPage.captured.displayOrder shouldBe 5
            }

            it("부모 페이지가 다른 스페이스에 속하면 NotFoundException (존재/소속 응답 통합)") {
                every { pageRepository.findBy(any()) } returns basicPage(spaceId = SpaceId(999L))

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(parentPageId = "99"))
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("INTERNAL space 안에 PUBLIC 페이지를 생성하면 ConflictException (cascade)") {
                every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.INTERNAL

                val exception =
                    shouldThrow<ConflictException> {
                        useCase.perform(basicRequest(visibility = "PUBLIC"))
                    }

                exception.errorCode shouldBe PageErrorCode.PAGE_VISIBILITY_EXCEEDS_SPACE
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("INTERNAL space 안에 MEMBER 페이지를 생성하면 ConflictException (cascade)") {
                every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.INTERNAL

                val exception =
                    shouldThrow<ConflictException> {
                        useCase.perform(basicRequest(visibility = "MEMBER"))
                    }

                exception.errorCode shouldBe PageErrorCode.PAGE_VISIBILITY_EXCEEDS_SPACE
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("INTERNAL space 안의 INTERNAL 페이지 생성은 통과한다") {
                every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.INTERNAL
                every { idGenerator.next() } returns 1L

                useCase.perform(basicRequest(visibility = "INTERNAL"))

                verify(exactly = 1) { pageRepository.save(any()) }
            }

            it("INTERNAL space 안의 DRAFT 페이지 생성은 항상 통과한다") {
                every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.INTERNAL
                every { idGenerator.next() } returns 1L

                useCase.perform(basicRequest(visibility = "DRAFT"))

                verify(exactly = 1) { pageRepository.save(any()) }
            }

            it("PUBLIC space 안의 PUBLIC 페이지 생성은 통과한다") {
                every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.PUBLIC
                every { idGenerator.next() } returns 1L

                useCase.perform(basicRequest(visibility = "PUBLIC"))

                verify(exactly = 1) { pageRepository.save(any()) }
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
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                spaceId = spaceId,
                parentPageId = parentPageId,
                title = title,
                content = content,
                visibility = visibility,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}
