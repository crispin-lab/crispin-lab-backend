package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageEditing.Request
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
import com.crispinlab.space.domain.page.Visibility
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
import java.time.Instant

class PageEditingUseCaseTest :
    DescribeSpec({
        val pageRepository = mockk<PageRepository>()
        val pageRevisionRepository = mockk<PageRevisionRepository>()
        val pageLinkRepository = mockk<PageLinkRepository>()
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val mentionDispatcher = mockk<MentionDispatcher>(relaxed = true)
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            PageEditingUseCase(
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
            every {
                spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
            } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)
            every { spaceRepository.findVisibility(any()) } returns SpaceVisibility.PUBLIC
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
                            content =
                                doc(
                                    paragraph(
                                        text("본문 "),
                                        pageLink(
                                            pageId = 11L,
                                            displayText = "wiki1"
                                        ),
                                        text(" "),
                                        pageLink(
                                            pageId = 12L,
                                            displayText = "wiki2"
                                        )
                                    )
                                )
                        )
                    )

                result.title shouldBe "새 제목"
                result.version shouldBe 2
                savedPage.captured.currentVersion shouldBe 2
                savedRevision.captured.version shouldBe 2
                savedLinks.captured shouldHaveSize 2
                savedLinks.captured.map { it.target } shouldContainExactly
                    listOf(PageId(11L), PageId(12L))
            }

            it("pageLink 없는 본문은 saveAll 이 빈 리스트로 호출된다") {
                val page = basicPage()
                every { pageRepository.findBy(page.id) } returns page
                every { idGenerator.next() } returnsMany listOf(101L)
                val savedLinks = slot<List<PageLink>>()
                every { pageLinkRepository.saveAll(capture(savedLinks)) } answers
                    { savedLinks.captured }

                useCase.perform(
                    basicRequest(
                        pageId = page.id.value.toString(),
                        content = doc(paragraph(text("단순 본문")))
                    )
                )

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
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("작성자더라도 멤버에서 추방됐으면 ForbiddenException") {
                val page = basicPage(authorId = UserId(100L))
                every { pageRepository.findBy(page.id) } returns page
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns null

                shouldThrow<ForbiddenException> {
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("ADMIN 은 작성자가 아니어도 수정 가능하다") {
                val page = basicPage(authorId = UserId(200L), title = "이전")
                every { pageRepository.findBy(page.id) } returns page
                every { idGenerator.next() } returnsMany listOf(101L)

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = page.id.value.toString(),
                            title = "새 제목",
                            content = "본문",
                            userId = UserId(100L),
                            isAdmin = true
                        )
                    )

                result.title shouldBe "새 제목"
                verify(exactly = 1) { pageRepository.save(any()) }
            }

            it("공개 범위를 같이 보내면 page.updatedAt 와 새 리비전의 createdAt 이 동일하다") {
                val page = basicPage(visibility = Visibility.DRAFT, currentVersion = 1)
                every { pageRepository.findBy(page.id) } returns page
                every { idGenerator.next() } returnsMany listOf(101L)
                val savedPage = slot<Page>()
                val savedRevision = slot<PageRevision>()
                every { pageRepository.save(capture(savedPage)) } answers { savedPage.captured }
                every { pageRevisionRepository.save(capture(savedRevision)) } answers
                    { savedRevision.captured }

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = page.id.value.toString(),
                            title = "새 제목",
                            content = "본문",
                            visibility = Visibility.PUBLIC.name
                        )
                    )

                savedPage.captured.visibility shouldBe Visibility.PUBLIC
                savedPage.captured.currentVersion shouldBe 2
                result.version shouldBe 2
                savedPage.captured.updatedAt shouldBe savedRevision.captured.createdAt
            }

            it("공개 범위를 생략하면 기존 값이 보존된다") {
                val page = basicPage(visibility = Visibility.INTERNAL)
                every { pageRepository.findBy(page.id) } returns page
                every { idGenerator.next() } returnsMany listOf(101L)
                val savedPage = slot<Page>()
                every { pageRepository.save(capture(savedPage)) } answers { savedPage.captured }

                useCase.perform(basicRequest(pageId = page.id.value.toString()))

                savedPage.captured.visibility shouldBe Visibility.INTERNAL
            }

            it("같은 공개 범위 + 같은 본문이면 page / revision / link 저장이 모두 skip 된다") {
                val page =
                    basicPage(
                        visibility = Visibility.INTERNAL,
                        currentVersion = 3
                    )
                val before: Instant = page.updatedAt
                every { pageRepository.findBy(page.id) } returns page

                useCase.perform(
                    basicRequest(
                        pageId = page.id.value.toString(),
                        title = page.title,
                        content = page.content.raw,
                        visibility = Visibility.INTERNAL.name
                    )
                )

                page.updatedAt shouldBe before
                page.currentVersion shouldBe 3
                verify(exactly = 0) { pageRepository.save(any()) }
                verify(exactly = 0) { pageRevisionRepository.save(any()) }
                verify(exactly = 0) { pageLinkRepository.saveAll(any()) }
                verify(exactly = 0) {
                    mentionDispatcher.dispatch(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                    )
                }
            }

            it("본문 변경 없이 공개 범위만 바뀌면 새 리비전을 만들지 않고 mention dispatch 도 skip 한다") {
                val page = basicPage(visibility = Visibility.DRAFT, currentVersion = 3)
                every { pageRepository.findBy(page.id) } returns page
                val savedPage = slot<Page>()
                every { pageRepository.save(capture(savedPage)) } answers { savedPage.captured }

                useCase.perform(
                    basicRequest(
                        pageId = page.id.value.toString(),
                        title = page.title,
                        content = page.content.raw,
                        visibility = Visibility.PUBLIC.name
                    )
                )

                savedPage.captured.visibility shouldBe Visibility.PUBLIC
                savedPage.captured.currentVersion shouldBe 3
                verify(exactly = 0) { pageRevisionRepository.save(any()) }
                verify(exactly = 0) { pageLinkRepository.saveAll(any()) }
                verify(exactly = 0) {
                    mentionDispatcher.dispatch(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                    )
                }
            }

            it("제목만 바뀐 편집은 새 리비전을 만들지만 mention dispatch 는 skip 한다") {
                val page =
                    basicPage(
                        visibility = Visibility.PUBLIC,
                        currentVersion = 1
                    )
                every { pageRepository.findBy(page.id) } returns page
                every { idGenerator.next() } returnsMany listOf(101L)

                useCase.perform(
                    basicRequest(
                        pageId = page.id.value.toString(),
                        title = "새 제목만",
                        content = page.content.raw,
                        visibility = null
                    )
                )

                verify(exactly = 1) { pageRevisionRepository.save(any()) }
                verify(exactly = 0) {
                    mentionDispatcher.dispatch(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                    )
                }
            }

            it("INTERNAL space 의 page 를 PUBLIC 으로 바꾸려 하면 ConflictException (cascade)") {
                val page = basicPage(visibility = Visibility.DRAFT)
                every { pageRepository.findBy(page.id) } returns page
                every { spaceRepository.findVisibility(page.spaceId) } returns
                    SpaceVisibility.INTERNAL

                val exception =
                    shouldThrow<ConflictException> {
                        useCase.perform(
                            basicRequest(
                                pageId = page.id.value.toString(),
                                title = page.title,
                                content = page.content.raw,
                                visibility = Visibility.PUBLIC.name
                            )
                        )
                    }

                exception.errorCode shouldBe PageErrorCode.PAGE_VISIBILITY_EXCEEDS_SPACE
                verify(exactly = 0) { pageRepository.save(any()) }
                verify(exactly = 0) { pageRevisionRepository.save(any()) }
                verify(exactly = 0) { pageLinkRepository.saveAll(any()) }
            }

            it("INTERNAL space 안의 page 라도 visibility 가 그대로면 cascade 검증 자체가 실행되지 않는다") {
                val page = basicPage(visibility = Visibility.INTERNAL)
                every { pageRepository.findBy(page.id) } returns page

                useCase.perform(
                    basicRequest(
                        pageId = page.id.value.toString(),
                        title = page.title,
                        content = page.content.raw,
                        visibility = Visibility.INTERNAL.name
                    )
                )

                verify(exactly = 0) { spaceRepository.findVisibility(any()) }
            }

            it(
                "recovery edit — space 가 좁아진 뒤 위반 상태로 남은 page 의 title/content 수정은 허용 (request.visibility=null)"
            ) {
                val page =
                    basicPage(visibility = Visibility.PUBLIC, currentVersion = 1)
                every { pageRepository.findBy(page.id) } returns page
                every { idGenerator.next() } returnsMany listOf(101L)
                val savedPage = slot<Page>()
                every { pageRepository.save(capture(savedPage)) } answers { savedPage.captured }

                useCase.perform(
                    basicRequest(
                        pageId = page.id.value.toString(),
                        title = "복구 수정",
                        content = "본문 정리",
                        visibility = null
                    )
                )

                savedPage.captured.visibility shouldBe Visibility.PUBLIC
                savedPage.captured.title shouldBe "복구 수정"
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "1",
            title: String = "새 제목",
            content: String = "새 본문",
            visibility: String? = null,
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                pageId = pageId,
                title = title,
                content = content,
                visibility = visibility,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}
